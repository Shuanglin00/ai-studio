package com.shuanglin.bot.langchain4j.config;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.config.DBMessageDTO;
import com.shuanglin.bot.langchain4j.rag.embedding.vo.EmbeddingEntity; // 假设 EmbeddingEntity 能够包含一个唯一的 ID 字段
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // 导入 SLF4J 日志
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import utils.FileReadUtil;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentInitializer {

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

	// 这些值应该从更灵活的来源获取，例如作为方法参数或通过Spring Security上下文
	// 这里暂时设为常量，但在实际应用中应避免硬编码
	private static final String DEFAULT_USER_ID = "default_user";
	private static final String DEFAULT_GROUP_ID = "default_group";

	@Resource
	MilvusClientV2 milvusClientV2;

	@Resource
	EmbeddingModel embeddingModel;

	@Resource
	MongoTemplate mongoTemplate;

	/**
	 * 读取指定路径的文件内容，处理后存储文本段落及其嵌入向量。
	 *
	 * @param file 要读取的文件。
	 * @return 插入数据的 Milvus 集合名称。
	 * @throws FileProcessingException 如果文件读取或处理过程中发生错误。
	 */
	public String readFile(File file) {
		if (file == null || !file.exists()) {
			log.error("尝试读取空文件或不存在的文件。");
			throw new FileProcessingException("文件不存在或为空。"); // 抛出自定义异常
		}
		try {
			String fileContent = FileReadUtil.readFileContent(file);
			log.info("成功读取文件内容，文件路径: {}", file.getAbsolutePath());
			readDocumentFromStr(fileContent);
		} catch (IOException | InvalidFormatException e) {
			log.error("读取文件 {} 时发生错误: {}", file.getAbsolutePath(), e.getMessage(), e);
			throw new FileProcessingException("文件处理失败: " + e.getMessage(), e); // 抛出自定义异常
		}
		return defaultCollectionName;
	}

	/**
	 * 从字符串内容中读取文档，分割，生成嵌入向量并存储。
	 *
	 * @param str 要处理的文档字符串。
	 */
	private void readDocumentFromStr(String str) {
		Document document = Document.from(str);
		// 使用递归分割器，分块大小 300，重叠 0
		DocumentSplitter recursive = DocumentSplitters.recursive(300, 0);

		// 将文档分割成 TextSegment，并为每个段落生成唯一的雪花ID作为其键
		Map<String, TextSegment> segmentMap = recursive.split(document).stream()
				.collect(Collectors.toMap(
						item -> IdUtil.getSnowflake().nextIdStr(), // keyMapper: 生成雪花ID作为键
						item -> item // valueMapper: 将当前 TextSegment 作为值
				));

		if (segmentMap.isEmpty()) {
			log.warn("文档分割后未产生任何文本段落。");
			return;
		}

		List<JsonObject> milvusInsertData = new ArrayList<>();
		List<DBMessageDTO> mongoUpsertData = new ArrayList<>();

		segmentMap.forEach((memoryId, textSegment) -> { // 将 key 重命名为 segmentId 更明确
			// 1. 生成嵌入向量
			log.debug("为段落 '{}' 生成嵌入向量...", textSegment.text());
			float[] vector = embeddingModel.embed(textSegment).content().vector();

			// 2. 准备 Milvus 插入数据
			// 确保 EmbeddingEntity 有一个用于Milvus主键的字段，例如 'id'，并将其设置为 segmentId
			// 假设 EmbeddingEntity.builder().id(segmentId) 或 .memoryId(segmentId) 是 Milvus 的主键
			JsonObject milvusObject = new Gson().toJsonTree(EmbeddingEntity.builder()
					.userId(DEFAULT_USER_ID)
					.groupId(DEFAULT_GROUP_ID)
					.embeddings(vector)
					.memoryId(memoryId) // memoryId 应该与 segmentId 保持一致，用于MongoDB关联
					.build()).getAsJsonObject();
			milvusInsertData.add(milvusObject);

			// 3. 准备 MongoDB 插入/更新数据
			DBMessageDTO dbMessage = new DBMessageDTO();
			dbMessage.setMemoryId(memoryId); // 使用雪花 ID 作为 memoryId
			dbMessage.setContent(textSegment.text()); // 只存储文本内容，而不是整个 TextSegment 对象
			dbMessage.setUserId(DEFAULT_USER_ID);
			dbMessage.setLastChatTime(System.currentTimeMillis());
			mongoUpsertData.add(dbMessage);
		});

		// 4. 批量插入 Milvus
		if (!milvusInsertData.isEmpty()) {
			try {
				log.info("开始向 Milvus 批量插入 {} 条数据到集合: {}", milvusInsertData.size(), defaultCollectionName);
				milvusClientV2.insert(InsertReq.builder()
						.collectionName(defaultCollectionName)
						.data(milvusInsertData) // 批量插入
						.build());
				log.info("成功向 Milvus 批量插入数据。");
			} catch (Exception e) {
				log.error("批量插入 Milvus 失败: {}", e.getMessage(), e);
				// 考虑在这里抛出异常或进行回滚
			}
		}

		// 5. 批量插入/更新 MongoDB
		if (!mongoUpsertData.isEmpty()) {
			try {
				log.info("开始向 MongoDB 批量 upsert {} 条数据。", mongoUpsertData.size());
				// MongoDB 批量 upsert 示例（更复杂，这里仅为示意）
				// 实际生产中，可能需要使用 BulkOperations 来实现真正的批量 upsert
				// 或者遍历 list 执行单个 upsert (如果数据量不大)
				for (DBMessageDTO message : mongoUpsertData) {
					Query query = new Query(Criteria.where("memoryId").is(message.getMemoryId()));
					Update update = new Update()
							.set("content", message.getContent())
							.set("userId", message.getUserId())
							.set("memoryId", message.getMemoryId());
							.set("lastChatTime", message.getLastChatTime());
					mongoTemplate.upsert(query, update, DBMessageDTO.class);
				}
				log.info("成功向 MongoDB 批量 upsert 数据。");
			} catch (Exception e) {
				log.error("批量 upsert MongoDB 失败: {}", e.getMessage(), e);
				// 考虑在这里抛出异常或进行回滚
			}
		}
	}
}

// 定义一个自定义运行时异常，用于文件处理错误
class FileProcessingException extends RuntimeException {
	public FileProcessingException(String message) {
		super(message);
	}

	public FileProcessingException(String message, Throwable cause) {
		super(message, cause);
	}
}