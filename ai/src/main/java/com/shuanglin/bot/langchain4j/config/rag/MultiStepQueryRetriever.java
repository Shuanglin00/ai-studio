package com.shuanglin.bot.langchain4j.config.rag;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shuanglin.bot.db.MessageStoreEntity;
import com.shuanglin.bot.langchain4j.assistant.DecomposeAssistant;
import com.shuanglin.bot.langchain4j.config.vo.MilvusProperties;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多步查询检索器
 *
 * @author lin
 * @date 2025/08/06
 */
@Slf4j
@Component("multiStepQueryRetriever")
@RequiredArgsConstructor
public class MultiStepQueryRetriever implements ContentRetriever {
	@Resource(name = "decomposeLanguageModel")
	private OllamaChatModel decomposeLanguageModel;

	private final MilvusClientV2 milvusClientV2;

	private final EmbeddingModel embeddingModel;

	private final MilvusProperties milvusProperties;

	private final MongoTemplate mongoTemplate;


	@Override
	public List<Content> retrieve(Query query) {
		//1. 查询分析-得多若干子查询
		String originalQuery = query.text();
		log.info("==================== [Multi-Step RAG 开始] ====================");
		log.info("[步骤 1] 接收到原始复杂查询: {}", originalQuery);

		// 步骤 2: 分解查询
		DecomposeAssistant assistant = AiServices.builder(DecomposeAssistant.class).chatModel(decomposeLanguageModel).build();
		String decomposeQuery = assistant.decompose(query.text());

		log.info("[步骤 2] LLM已将查询分解为以下子问题:");
		List<TextSegment> subQueries = Arrays.stream(decomposeQuery.split("\n")).toList().stream().map(TextSegment::from).toList();
		subQueries.stream().forEach(item->{
			log.info("问题: {}", item.text());
		});
		// 步骤 3 & 4: 对每个子查询进行检索并聚合结果
		// 使用Set来自动处理重复的Content
		Set<Content> allRelevantContent = new LinkedHashSet<>();

		log.info("[步骤 3] 正在对每个子问题进行并行检索...");
		List<Embedding> content = embeddingModel.embedAll(subQueries).content();
		List<BaseVector> floatVecs = content.stream().map(item-> new FloatVec(item.vector())).collect(Collectors.toList());
		SearchReq searchRequest = SearchReq.builder()
				.databaseName(milvusProperties.getDefaultDatabaseName())
				.collectionName(milvusProperties.getDefaultCollectionName())
				.data(floatVecs)
				.topK(milvusProperties.getTopK())
				.build();
		SearchResp vecSearch = milvusClientV2.search(searchRequest);
		List<String> messageIds = vecSearch.getSearchResults().stream()
				.flatMap(Collection::stream)
				.map(match -> match.getId().toString()) // 假设 ID 是字符串类型
				.collect(Collectors.toList());

		log.info("[Step 3] Extracted memory IDs from search result: {}", messageIds);

		// -------------------- 步骤 3: 从 Milvus 结果中提取 memoryId --------------------
		org.springframework.data.mongodb.core.query.Query mongoQuery = new org.springframework.data.mongodb.core.query.Query();
		mongoQuery.addCriteria(Criteria.where("messageId").in(messageIds));
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
					return Content.from(TextSegment.textSegment(document.getContent() == null ? "" : document.getContent()));
				})
				.collect(Collectors.toList());

		log.info("[Step 5] Mapping complete. Returning {} Content objects.", finalContentList.size());
		log.info("==================== [END] Retrieval Process (Success) ====================");
		log.info("==================== [Multi-Step RAG 结束] ====================");

		return new ArrayList<>(finalContentList);


	}
}
