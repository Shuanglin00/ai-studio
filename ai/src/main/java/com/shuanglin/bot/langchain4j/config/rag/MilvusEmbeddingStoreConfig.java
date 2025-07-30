package com.shuanglin.bot.langchain4j.config.rag;

import com.shuanglin.bot.langchain4j.config.vo.MilvusProperties;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties({MilvusProperties.class})
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
	public MilvusClientV2 milvusClient(MilvusProperties milvusProperties) {
		ConnectConfig config = ConnectConfig.builder()
				.uri(milvusProperties.getUrl())
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
