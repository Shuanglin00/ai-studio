package com.shuanglin.bot.langchain4j.config.rag;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.shuanglin.bot.db.MessageStoreEntity;
import com.shuanglin.bot.langchain4j.config.store.DbQueryVO;
import com.shuanglin.bot.langchain4j.config.vo.MilvusProperties;
import com.shuanglin.dao.model.Model;
import com.shuanglin.dao.model.ModelsRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component("filterQueryRetriever")
@RequiredArgsConstructor
public class FilterQueryRetriever implements ContentRetriever {

	private final MilvusClientV2 milvusClient;

	private final EmbeddingModel embeddingModel;

	private final MongoTemplate mongoTemplate;

	private final ModelsRepository modelsRepository;

	private final Gson gson;

	private final MilvusProperties milvusProperties;

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

	/**
	 * 检索
	 * <p>
	 * a. 将query转换为向量<br>
	 * b. 从milvus中获取原数据Id<br>
	 * c. 用这些ID查询你的主数据库MongoDB。<br>
	 * d. 将从主数据库查到的完整、最新对象格式化成文本。<br>
	 * e. 将格式化后的文本封装成 Content 对象列表返回。
	 *
	 * @param query 查询
	 * @return {@code List<Content> }
	 */
	@Override
	public List<Content> retrieve(Query query) {
		// 检查query是否包含SysMessage
		JsonObject params = gson.toJsonTree(query.metadata().chatMemoryId()).getAsJsonObject();
		JsonObject queryParams = gson.toJsonTree(gson.fromJson(params, DbQueryVO.class)).getAsJsonObject();
		JsonObject senderInfo = gson.toJsonTree(params.get("senderInfo")).getAsJsonObject();
		JsonObject group = gson.toJsonTree(params.get("groupMessageEvent")).getAsJsonObject();
		String modelName = senderInfo.get("modelInfo").getAsJsonObject().get("modelName").getAsString();
		Model model = modelsRepository.getModelByModelName(modelName);
		Map<String, Object> promptMap = gson.fromJson(gson.toJsonTree(model).getAsJsonObject(), new TypeToken<Map<String, Object>>() {}.getType());
		log.info("==================== [START] Retrieval Process ====================");
		// 消息 自动凭借 存入数据库需要根据相同memoryId覆盖 多个memory
		try {
			// -------------------- 步骤 1: 向量化查询文本 --------------------
			String question = query.text();
			log.info("[Step 1] Input query text: '{}'", question);

			Embedding queryEmbedding = embeddingModel.embed(question).content();
			log.info("[Step 1] Query text successfully converted to vector. Dimension: {}", queryEmbedding.vector().length);
			// -------------------- 步骤 2: 在 Milvus 中进行向量搜索 --------------------
			FloatVec floatVec = new FloatVec(queryEmbedding.vectorAsList());
			SearchReq searchRequest = SearchReq.builder()
					.databaseName(defaultDatabaseName)
					.collectionName(defaultCollectionName)
					.filterTemplateValues(gson.fromJson(queryParams, new TypeToken<Map<String, Object>>() {
					}.getType()))
					.data(Collections.singletonList(floatVec))
					.topK(milvusProperties.getTopK())
					.build();

			SearchResp searchResult = milvusClient.search(searchRequest);
			log.info("[Step 2] Vector search completed. Found {} potential matches.", searchResult.getSearchResults().size());

			// -------------------- 步骤 3: 从 Milvus 结果中提取 memoryId --------------------
			if (searchResult.getSearchResults().isEmpty()) {
				log.warn("[Step 3] No matches found in vector store. Aborting retrieval.");
				log.info("==================== [END] Retrieval Process (No Results) ====================");
				return List.of();
			}
			// **【关键修正】** 从 EmbeddingMatch 中提取 ID 列表
			List<String> messageIds = searchResult.getSearchResults().stream()
					.flatMap(Collection::stream)
					.map(match -> match.getId().toString()) // 假设 ID 是字符串类型
					.collect(Collectors.toList());
			log.info("[Step 3] Extracted memory IDs from search result: {}", messageIds);

			org.springframework.data.mongodb.core.query.Query mongoQuery = new org.springframework.data.mongodb.core.query.Query();
			for (Map.Entry<String, JsonElement> entry : queryParams.entrySet()) {
				mongoQuery.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue().getAsString()));
			}
			List<MessageStoreEntity> mongoResult = mongoTemplate.find(mongoQuery, MessageStoreEntity.class);
			// -------------------- 步骤 4: 使用 memoryId 从 MongoDB 获取原始数据 --------------------


			log.info("[Step 4] MongoDB query completed. Found {} full documents.", mongoResult.size());

			if (mongoResult.isEmpty()) {
				log.warn("[Step 4] MongoDB returned no documents for the given IDs. Check for data consistency issues.");
				log.info("==================== [END] Retrieval Process (No Docs) ====================");
				return List.of();
			}

			// -------------------- 步骤 5: 将数据库结果转换为 LangChain4j 的 Content 格式 --------------------
			List<Content> finalContentList = mongoResult.stream()
					.map(document -> {
						// 在这里，你可以从 document 构建非常丰富的元数据
						return Content.from(TextSegment.textSegment(document.getContent() ==null?"":document.getContent(), new Metadata(promptMap)));
					})
					.collect(Collectors.toList());

			log.info("[Step 5] Mapping complete. Returning {} Content objects.", finalContentList.size());
			log.info("==================== [END] Retrieval Process (Success) ====================");

			return finalContentList;
		} catch (Exception e) {
			log.error("An unexpected error occurred during the retrieval process.", e);
			log.info("==================== [END] Retrieval Process (Error) ====================");
			return List.of(); // 在异常情况下返回空列表
		}
	}

}
