package com.shuanglin.bot.langchain4j.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusEmbeddingStoreConfig {
/*	*//**
	 * 在内存中嵌入存储
	 *
	 * @return {@code EmbeddingStore<TextSegment> }
	 *//*
	@Bean
	public EmbeddingStore<TextSegment> inMemoryEmbeddingStore() {
		return new InMemoryEmbeddingStore<>();
	}*/

	@Bean
	public MilvusEmbeddingStore milvusEmbeddingStore() {
		// 替换为你的Milvus连接参数
		return MilvusEmbeddingStore.builder()
				.host("172.18.32.160")
				.port(19530)
				.collectionName("rag_embedding_collection")
				.build();
	}
	@Bean
	public ContentRetriever milvusContentRetriever(MilvusEmbeddingStore milvusEmbeddingStore,
	                                               EmbeddingModel embeddingModel) {
		return EmbeddingStoreContentRetriever.builder()
				.embeddingStore(milvusEmbeddingStore)
				.embeddingModel(embeddingModel)
				.build();
	}
}
