package com.shuanglin.bot.langchain4j.rag.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.db.KnowledgeEntity;
import com.shuanglin.bot.db.KnowledgeEntityRepository;
import com.shuanglin.dao.Model;
import com.shuanglin.dao.ModelsRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
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
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component("NonMemoryRetriever")
public class NonMemoryRetriever implements ContentRetriever {

	private final MilvusClientV2 milvusClient;

	private final EmbeddingModel embeddingModel;

	private final KnowledgeEntityRepository knowledgeRepository;

	private final ModelsRepository modelsRepository;

	private final Gson gson;

	private final PromptTemplate promptTemplate;

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

	/**
	 * 检索
	 * <p>
	 * a. 调用 embeddingStore.findRelevant() 获取包含元数据的结果。
	 * b. 从元数据中提取主键ID。
	 * c. 用这些ID查询你的主数据库（MySQL/Mongo等）。
	 * d. 将从主数据库查到的完整、最新对象格式化成文本。
	 * e. 将格式化后的文本封装成 Content 对象列表返回。
	 *
	 * @param query 查询
	 * @return {@code List<Content> }
	 */
	@Override
	public List<Content> retrieve(Query query) {
		// 检查query是否包含SysMessage
		JsonObject params = new JsonObject();
		params = gson.toJsonTree(query.metadata().chatMemoryId()).getAsJsonObject();
		JsonObject senderInfo = gson.toJsonTree(params.get("senderInfo")).getAsJsonObject();
		JsonObject group = gson.toJsonTree(params.get("groupMessageEvent")).getAsJsonObject();
		String groupId = group.get("group_id").getAsString();
		String userId = group.get("user_id").getAsString();
		String modelId = senderInfo.get("modelInfo").getAsJsonObject().get("useModel").getAsString();
		Model model = modelsRepository.getModelById(modelId);
		log.info("==================== [START] Retrieval Process ====================");
		// 消息 自动凭借 存入数据库需要根据相同memoryId覆盖 多个memory
		try {
			// -------------------- 步骤 1: 向量化查询文本 --------------------
			String queryText = query.text();
			log.info("[Step 1] Input query text: '{}'", queryText);

			Embedding queryEmbedding = embeddingModel.embed(queryText).content();
			log.info("[Step 1] Query text successfully converted to vector. Dimension: {}", queryEmbedding.vector().length);
			// 对于调试，可以打印向量的前几个维度
			// log.debug("[Step 1] Query vector (first 5 dims): {}", Arrays.toString(Arrays.copyOf(queryEmbedding.vector(), 5)));

			// -------------------- 步骤 2: 在 Milvus/EmbeddingStore 中进行向量搜索 --------------------
			FloatVec floatVec = new FloatVec(queryEmbedding.vectorAsList());
			int maxResults = 5;
			Map<String, Object> searchParams = new HashMap<>();
			searchParams.put("groupId", groupId);
			searchParams.put("userId", userId);
			searchParams.put("modelId", modelId);
			SearchReq searchRequest = SearchReq.builder()
					.databaseName(defaultDatabaseName)
					.collectionName(defaultCollectionName)
					.filterTemplateValues(searchParams)
					.data(Collections.singletonList(floatVec))
					.topK(maxResults)
					.build();

			log.info("[Step 2] Executing vector search in EmbeddingStore with maxResults={}", maxResults);

			SearchResp searchResult = milvusClient.search(searchRequest);
			log.info("[Step 2] Vector search completed. Found {} potential matches.", searchResult.getSearchResults().size());

			// 打印每个匹配项的详细信息，这是调试的关键！
			searchResult.getSearchResults().forEach(match ->
					log.info("[Step 2] Vector search result: {}", match)
			);

			// -------------------- 步骤 3: 从 Milvus 结果中提取 memoryId --------------------
			if (searchResult.getSearchResults().isEmpty()) {
				log.warn("[Step 3] No matches found in vector store. Aborting retrieval.");
				log.info("==================== [END] Retrieval Process (No Results) ====================");
				return List.of();
			}
			System.out.println("searchResult.getSearchResults() = " + searchResult.getSearchResults());
			// **【关键修正】** 从 EmbeddingMatch 中提取 ID 列表
			List<String> messageIds = searchResult.getSearchResults().get(0).stream()
					.map(match -> match.getId().toString()) // 假设 ID 是字符串类型
					.collect(Collectors.toList());
			log.info("[Step 3] Extracted memory IDs from search result: {}", messageIds);


			// -------------------- 步骤 4: 使用 memoryId 从 MongoDB 获取原始数据 --------------------

			List<KnowledgeEntity> knowledge = knowledgeRepository.findByGroupIdAndUserIdAndModelId(groupId, userId, modelId);
			log.info("[Step 4] MongoDB query completed. Found {} full documents.", knowledge.size());

			if (knowledge.isEmpty()) {
				log.warn("[Step 4] MongoDB returned no documents for the given IDs. Check for data consistency issues.");
				log.info("==================== [END] Retrieval Process (No Docs) ====================");
				return List.of();
			}

			// -------------------- 步骤 5: 将数据库结果转换为 LangChain4j 的 Content 格式 --------------------
			log.info("[Step 5] Mapping {} DB documents to LangChain4j Content objects.", knowledge.size());
			List<Content> finalContentList = knowledge.stream()
					.map(document -> {
						// 在这里，你可以从 document 构建非常丰富的元数据
						String content = document.getContent();
						return Content.from(content);
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
