package com.shuanglin.bot.langchain4j.config.rag;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {
//	/**
//	 * 默认的milvus嵌入存储内容检索器
//	 *
//	 * 接收用户查询 (Query)：当 AiService 或 ChatClient 使用这个 ContentRetriever 时，它会接收到用户的原始文本问题。
//	 * 查询向量化 (Embed Query)：它会使用您注入的 embeddingModel，将用户的文本问题转换成一个查询向量。
//	 * 向量搜索 (Search)：它会拿着这个查询向量，调用您注入的 embeddingStore 的 .search() 或 .findRelevant() 方法，在底层向量数据库（如 Milvus）中执行相似度搜索。
//	 * 过滤与排序 (Filter & Rank)：它会根据您设置的 maxResults(10) 和 minScore(0.50) 对搜索结果进行筛选和排序。
//	 * 返回内容 (Return Content)：最终，它会返回一个 List<Content> 或 List<TextSegment>，这些就是从向量数据库中直接检索出来的文本内容。
//	 * 注入 LLM：框架的上层（如 RetrievalAugmentor）会将这些返回的文本内容注入到发送给 LLM 的最终提示（Prompt）中。
//	 *
//	 * @param embeddingModel 嵌入模型
//	 * @param embeddingStore 嵌入存储
//	 * @return {@code ContentRetriever }
//	 */
//	@Bean
//	@Primary
//	public ContentRetriever embeddingStoreContentRetriever(EmbeddingModel embeddingModel,
//	                                                       EmbeddingStore<TextSegment> embeddingStore) {
//
//
//		return EmbeddingStoreContentRetriever.builder()
//				.embeddingStore(embeddingStore)
//				.embeddingModel(embeddingModel)
//				//最大返回数量，可以理解为 limit 10
//				.maxResults(10)
//				//最小匹配分数，可以理解为 where score >= 0.5
//				.minScore(0.50)
//				.build();
//	}
//
//
//	@Bean
//	public RetrievalAugmentor retrievalAugmentor(ContentRetriever milvusContentRetriever,
//	                                             DefaultContentInjector defaultContentInjector) {
//		DefaultQueryTransformer queryTransformer = new DefaultQueryTransformer();
////		QueryRouter queryRouter = new SwitchQueryRouter(milvusContentRetriever);
//		DefaultContentAggregator contentAggregator = new DefaultContentAggregator();
//
//		return DefaultRetrievalAugmentor.builder()
//				.queryTransformer(queryTransformer)
//				.queryRouter(new DefaultQueryRouter())
//				.contentAggregator(contentAggregator)
//				.contentInjector(defaultContentInjector)
//				.build();
//	}
}
