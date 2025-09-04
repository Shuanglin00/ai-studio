package com.shuanglin.bot.langchain4j.rag.retriever;

import com.shuanglin.bot.langchain4j.assistant.DecomposeAssistant;
import com.shuanglin.bot.langchain4j.config.vo.MilvusProperties;
import com.shuanglin.bot.langchain4j.rag.util.MilvusClusterCenterFinder;
import com.shuanglin.dao.message.MessageStoreEntity;
import com.shuanglin.dao.message.MessageStoreEntityRepository;
import com.shuanglin.dao.milvus.MessageEmbeddingMapper;
import com.shuanglin.enums.MongoDBConstant;
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
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("contentQueryRetriever")
@RequiredArgsConstructor
public class ContentQueryRetriever implements ContentRetriever {
	private final MilvusClientV2 milvusClientV2;
	private final MessageEmbeddingMapper messageEmbeddingMapper;
	private final EmbeddingModel embeddingModel;
	private final MessageStoreEntityRepository messageStoreEntityRepository;
	private final MilvusProperties milvusProperties;
	private final MongoTemplate mongoTemplate;
	@Resource(name = "decomposeLanguageModel")
	private OllamaChatModel decomposeLanguageModel;

	@Override
	public List<Content> retrieve(Query query) {
		//1. 查询分析-得多若干子查询
		log.info("==================== [Multi-Step RAG 开始] ====================");
		Embedding content = embeddingModel.embed(TextSegment.textSegment(query.text())).content();
		SearchReq searchRequest = SearchReq.builder()
				.filterTemplateValues(Map.of("storeType", MongoDBConstant.StoreType.document.name()))
				.data(Collections.singletonList(new FloatVec(content.vector())))
				.topK(milvusProperties.getTopK())
				.build();
		SearchResp vecSearch = messageEmbeddingMapper.getClient().search(searchRequest);
		List<String> messageIds = vecSearch.getSearchResults().stream()
				.flatMap(Collection::stream)
				.map(it -> it.getEntity().get("storeId").toString())
				.collect(Collectors.toList());

		log.info("[Step 3] Extracted memory IDs from search result: {}", messageIds);

		// -------------------- 步骤 3: 从 Milvus 结果中提取 memoryId --------------------
		List<MessageStoreEntity> mongoResult = messageStoreEntityRepository.findAllById(messageIds);
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
				.toList();

		log.info("[Step 5] Mapping complete. Returning {} Content objects.", finalContentList.size());
		log.info("==================== [Multi-Step RAG 结束] ====================");

		return new ArrayList<>(finalContentList);
	}
}
