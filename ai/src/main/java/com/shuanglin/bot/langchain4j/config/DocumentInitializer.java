package com.shuanglin.bot.langchain4j.config;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.config.DBMessageDTO;
import com.shuanglin.bot.langchain4j.config.rag.embedding.vo.EmbeddingEntity;
import com.shuanglin.bot.utils.FileReadUtil;
import com.shuanglin.bot.utils.ProjectReaderUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.github.admin4j.http.HttpRequest;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


@Service
@Slf4j
public class DocumentInitializer {

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

	@Resource
	MilvusClientV2 milvusClientV2;

	@Resource
	EmbeddingModel embeddingModel;

	@Resource
	MongoTemplate mongoTemplate;

	/**
	 * 读取单个文件内容，将其作为一个整体处理，生成向量并存储。
	 *
	 * @param request 用于获取 sender 和 groupId 的 HttpRequest 对象。
	 * @param file    要读取的 File 对象。
	 * @return 默认集合名。
	 */
	public String readFile(HttpRequest request, File file) {
		if (file == null || !file.exists()) {
			log.error("尝试读取空文件或不存在的文件: {}", file != null ? file.getAbsolutePath() : "null");
			return defaultCollectionName;
		}

		// 检查是否是 ZIP 文件，如果是，调用处理 ZIP 的方法
		if (file.getName().toLowerCase().endsWith(".zip")) {
			log.info("检测到 ZIP 文件，调用 processZipFile 方法处理: {}", file.getAbsolutePath());
			return processZipFile(request, file);
		}

		// 如果是普通文件 (txt, java, etc.)，则按单个文件处理
		try {
			String fileContent = FileReadUtil.readFileContent(file); // 使用原有的 FileReadUtil 读取内容
			log.info("成功读取单个文件内容，文件路径: {}", file.getAbsolutePath());
			// 将整个文件内容作为一个独立单元处理
			processSingleContent(request, fileContent, file.getName());
		} catch (IOException | InvalidFormatException e) {
			log.error("读取单个文件 {} 时发生错误: {}", file.getAbsolutePath(), e.getMessage(), e);
		}
		return defaultCollectionName;
	}

	/**
	 * 将给定的字符串作为整个文档处理，生成向量并存储。
	 * @param request 用于获取 sender 和 groupId 的 HttpRequest 对象。
	 * @param str 要处理的文档字符串。
	 */
	public void learnStr(HttpRequest request, String str) {
		// 使用一个通用的标识符来代表这个字符串，例如 "string_content"
		processSingleContent(request, str, "string_content");
	}

	/**
	 * 处理 ZIP 文件中的所有文本文件，将每个文件作为一个独立的文本段落处理，
	 * 为每个文件生成唯一的 ID，嵌入向量并存储。
	 *
	 * @param request 用于获取 sender 和 groupId 的 HttpRequest 对象。
	 * @param zipFile 要读取的 ZIP 文件。
	 * @return 默认集合名。
	 */
	public String processZipFile(HttpRequest request, File zipFile) {
		// ... (这里是之前 processZipFile 方法的代码) ...
		// 确保这个方法的内容是完整的，如下面的 "processZipFile 方法的完整代码" 部分

		String userId = getUserIdFromRequest(request);
		String groupId = getGroupIdFromRequest(request);

		List<JsonObject> milvusInsertData = new ArrayList<>();
		List<DBMessageDTO> mongoUpsertData = new ArrayList<>();

		try (ZipFile zip = new ZipFile(zipFile)) {
			java.util.Enumeration<? extends ZipEntry> entries = zip.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				if (!entry.isDirectory()) {
					String entryName = entry.getName();
					String lowerCaseEntryName = entryName.toLowerCase();

					if (ProjectReaderUtil.isTextFile(entryName, lowerCaseEntryName)) { // 调用公共的 isTextFile 方法
						log.info("处理 ZIP 内的文本文件: {}", entryName);

						String fileContent;
						try (InputStream entryInputStream = zip.getInputStream(entry)) {
							fileContent = new String(entryInputStream.readAllBytes(), StandardCharsets.UTF_8);
						} catch (IOException e) {
							log.error("读取 ZIP 内文件 {} 内容时发生错误: {}", entryName, e.getMessage(), e);
							continue; // 跳过此文件
						}

						if (fileContent.trim().isEmpty()) {
							log.warn("文件 {} 内容为空，跳过处理。", entryName);
							continue;
						}

						// 为每个文件生成独立的 ID 和处理
						processSingleContent(request, fileContent, entryName, milvusInsertData, mongoUpsertData);

					} else {
						log.debug("跳过非文本文件: {}", entryName);
					}
				}
			}
		} catch (IOException e) {
			log.error("读取 ZIP 文件 {} 时发生错误: {}", zipFile.getAbsolutePath(), e.getMessage(), e);
			return defaultCollectionName;
		}

		// 批量插入 Milvus
		if (!milvusInsertData.isEmpty()) {
			try {
				log.info("开始向 Milvus 批量插入 {} 个文件的数据到集合: {}", milvusInsertData.size(), defaultCollectionName);
				milvusClientV2.insert(InsertReq.builder()
						.collectionName(defaultCollectionName)
						.data(milvusInsertData)
						.build());
				log.info("成功向 Milvus 批量插入 {} 个文件的数据。", milvusInsertData.size());
			} catch (Exception e) {
				log.error("批量插入 Milvus 失败: {}", e.getMessage(), e);
			}
		}

		// 批量插入/更新 MongoDB
		if (!mongoUpsertData.isEmpty()) {
			try {
				log.info("开始向 MongoDB 批量 upsert {} 条数据。", mongoUpsertData.size());
				for (DBMessageDTO message : mongoUpsertData) {
					Query query = new Query(Criteria.where("memoryId").is(message.getMemoryId()));
					Update update = new Update()
							.set("content", message.getContent())
							.set("userId", message.getUserId())
							.set("groupId", message.getGroupId())
							.set("memoryId", message.getMemoryId())
							.set("lastChatTime", message.getLastChatTime());
					mongoTemplate.upsert(query, update, DBMessageDTO.class);
				}
				log.info("成功向 MongoDB 批量 upsert {} 条数据。", mongoUpsertData.size());
			} catch (Exception e) {
				log.error("批量 upsert MongoDB 失败: {}", e.getMessage(), e);
			}
		}

		return defaultCollectionName;
	}


	/**
	 * 通用的方法，处理单个文本内容（来自文件或字符串），将其作为一个整体生成向量并存储。
	 * 这个方法被 readFile 和 learnStr 调用，也用于 processZipFile 内部处理每个文本文件。
	 *
	 * @param request           用于获取 sender 和 groupId 的 HttpRequest 对象。
	 * @param content           要处理的文本内容。
	 * @param contentIdentifier 用于生成唯一ID的标识符（如文件名或 "string_content"）。
	 */
	private void processSingleContent(HttpRequest request, String content, String contentIdentifier) {
		// 这里先定义两个空的 List，用于兼容 processZipFile 的调用方式
		// 在实际调用时，如果不是 processZipFile 那么这些 List 将不会被填充
		List<JsonObject> milvusInsertData = new ArrayList<>();
		List<DBMessageDTO> mongoUpsertData = new ArrayList<>();
		processSingleContent(request, content, contentIdentifier, milvusInsertData, mongoUpsertData);

		// 如果是独立调用 readFile 或 learnStr，则需要在此处执行批量存储
		if (!milvusInsertData.isEmpty()) {
			try {
				log.info("开始向 Milvus 批量插入 {} 条数据到集合: {}", milvusInsertData.size(), defaultCollectionName);
				milvusClientV2.insert(InsertReq.builder()
						.collectionName(defaultCollectionName)
						.data(milvusInsertData)
						.build());
				log.info("成功向 Milvus 批量插入数据。");
			} catch (Exception e) {
				log.error("批量插入 Milvus 失败: {}", e.getMessage(), e);
			}
		}

		if (!mongoUpsertData.isEmpty()) {
			try {
				log.info("开始向 MongoDB 批量 upsert {} 条数据。", mongoUpsertData.size());
				for (DBMessageDTO message : mongoUpsertData) {
					Query query = new Query(Criteria.where("memoryId").is(message.getMemoryId()));
					Update update = new Update()
							.set("content", message.getContent())
							.set("userId", message.getUserId())
							.set("groupId", message.getGroupId())
							.set("memoryId", message.getMemoryId())
							.set("lastChatTime", message.getLastChatTime());
					mongoTemplate.upsert(query, update, DBMessageDTO.class);
				}
				log.info("成功向 MongoDB 批量 upsert 数据。");
			} catch (Exception e) {
				log.error("批量 upsert MongoDB 失败: {}", e.getMessage(), e);
			}
		}
	}

	/**
	 * 处理单个文本内容的核心方法，将内容添加到批量操作的列表中。
	 *
	 * @param request           用于获取 sender 和 groupId 的 HttpRequest 对象。
	 * @param content           要处理的文本内容。
	 * @param contentIdentifier 用于生成唯一ID的标识符（如文件名或 "string_content"）。
	 * @param milvusInsertData  待插入 Milvus 的数据列表。
	 * @param mongoUpsertData   待 upsert MongoDB 的数据列表。
	 */
	private void processSingleContent(HttpRequest request, String content, String contentIdentifier, List<JsonObject> milvusInsertData, List<DBMessageDTO> mongoUpsertData) {
		if (content == null || content.trim().isEmpty()) {
			log.warn("内容为空，无法进行处理: {}", contentIdentifier);
			return;
		}

		String userId = getUserIdFromRequest(request);
		String groupId = getGroupIdFromRequest(request);

		// 为每个文件生成一个唯一的 ID (memoryId)
		String segmentId = IdUtil.getSnowflake().nextIdStr();

		// 创建一个 TextSegment 来包装整个内容
		// metadata 中可以包含更多信息，例如 originalFileName
		TextSegment textSegment = TextSegment.from(content);

		// 1. 生成嵌入向量
		log.debug("为内容 '{}' 生成嵌入向量...", contentIdentifier);
		float[] vector = embeddingModel.embed(textSegment).content().vector();

		// 2. 准备 Milvus 插入数据
		JsonObject milvusObject = new Gson().toJsonTree(EmbeddingEntity.builder()
				.userId(userId)
				.groupId(groupId)
				.embeddings(vector)
				.memoryId(segmentId) // 使用为文件/内容生成的唯一 ID
				.build()).getAsJsonObject();
		milvusInsertData.add(milvusObject);

		// 3. 准备 MongoDB 插入/更新数据
		DBMessageDTO dbMessage = new DBMessageDTO();
		dbMessage.setMemoryId(segmentId); // 使用生成的唯一 ID
		dbMessage.setContent(content); // 存储整个文件的文本内容
		dbMessage.setUserId(userId);
		dbMessage.setGroupId(groupId);
		dbMessage.setLastChatTime(System.currentTimeMillis());
		mongoUpsertData.add(dbMessage);
	}

	/**
	 * 从 HttpRequest 中安全地获取 userId。
	 *
	 * @param request HttpRequest 对象。
	 * @return 用户 ID，如果请求或 header 为空，则返回默认值 "unknown_user"。
	 */
	private String getUserIdFromRequest(HttpRequest request) {
		String userId = null;
		if (request != null && request.getHeaders() != null) {
			userId = request.getHeaders().get("sender").toString();
		}
		if (userId == null || userId.isEmpty()) {
			log.warn("HttpRequest 中未找到 'sender' 头，将使用默认值 'unknown_user'。");
			return "999";
		}
		return userId;
	}

	/**
	 * 从 HttpRequest 中安全地获取 groupId。
	 *
	 * @param request HttpRequest 对象。
	 * @return 群组 ID，如果请求或 header 为空，则返回默认值 "unknown_group"。
	 */
	private String getGroupIdFromRequest(HttpRequest request) {
		String groupId = null;
		if (request != null && request.getHeaders() != null) {
			groupId = request.getHeaders().get("groupId").toString();
		}
		if (groupId == null || groupId.isEmpty()) {
			log.warn("HttpRequest 中未找到 'groupId' 头，将使用默认值 'unknown_group'。");
			return "999";
		}
		return groupId;
	}

	// 如果 ProjectReaderUtil 是 public 的，那么 DocumentInitializer 可以直接调用
	// private static boolean isTextFile(String entryName, String lowerCaseEntryName) {
	//     return ProjectReaderUtil.isTextFile(entryName, lowerCaseEntryName);
	// }
}