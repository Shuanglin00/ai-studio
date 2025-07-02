package com.shuanglin.bot.langchain4j.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Configuration
public class RagConfiguration {
	@Bean
	@Primary
	public ContentRetriever embeddingStoreContentRetriever(EmbeddingModel embeddingModel,
	                                                       EmbeddingStore<TextSegment> embeddingStore) {


		return EmbeddingStoreContentRetriever.builder()
				.embeddingStore(embeddingStore)
				.embeddingModel(embeddingModel)
				//最大返回数量，可以理解为 limit 10
				.maxResults(10)
				//最小匹配分数，可以理解为 where score >= 0.5
				.minScore(0.50)
				//.filter()
				//  dynamicFilter方法，用户与查询有关的过滤条件。例如：知识库中的文档有权限限制，根据每个人的权限查询出不同的文档
				.dynamicFilter(query -> {
					//等价于where scope >= 0
					return metadataKey("scope").isGreaterThanOrEqualTo(0);
				})
				.build();
	}


	@Bean
	public RetrievalAugmentor retrievalAugmentor(ContentRetriever milvusContentRetriever,
	                                             DefaultContentInjector defaultContentInjector) {
		DefaultQueryTransformer queryTransformer = new DefaultQueryTransformer();
//		QueryRouter queryRouter = new SwitchQueryRouter(milvusContentRetriever);
		DefaultContentAggregator contentAggregator = new DefaultContentAggregator();

		return DefaultRetrievalAugmentor.builder()
				.queryTransformer(queryTransformer)
				.queryRouter(new DefaultQueryRouter())
				.contentAggregator(contentAggregator)
				.contentInjector(defaultContentInjector)
				.build();
	}
}
