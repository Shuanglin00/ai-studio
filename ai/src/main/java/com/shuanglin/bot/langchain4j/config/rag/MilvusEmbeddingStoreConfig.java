package com.shuanglin.bot.langchain4j.config.rag;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusEmbeddingStoreConfig {

	@Value("${spring.data.milvus.url}")
	private String url;

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

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
	public MilvusClientV2 milvusClient(){
		ConnectConfig config = ConnectConfig.builder()
				.uri(url)
				.build();

		return new MilvusClientV2(config);
	}

//	@Bean
//	public MilvusEmbeddingStore milvusEmbeddingStore() {
//		// 替换为你的Milvus连接参数
//		return MilvusEmbeddingStore.builder()
//				.host(host)
//				.port(port)
//				.databaseName(defaultDatabaseName)
//				.dimension(384)
//				.collectionName(defaultCollectionName)
//				.build();
//	}
//	@Bean
//	public ContentRetriever milvusContentRetriever(MilvusEmbeddingStore milvusEmbeddingStore,
//	                                               EmbeddingModel embeddingModel) {
//		return EmbeddingStoreContentRetriever.builder()
//				.embeddingStore(milvusEmbeddingStore)
//				.embeddingModel(embeddingModel)
//				.build();
//	}
}
