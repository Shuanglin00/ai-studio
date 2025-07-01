package com.shuanglin.bot.langchain4j.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
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
				.host("localhost")
				.port(19530)
				.collectionName("your_collection")
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
