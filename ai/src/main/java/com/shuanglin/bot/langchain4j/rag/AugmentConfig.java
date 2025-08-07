package com.shuanglin.bot.langchain4j.rag;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AugmentConfig {

	@Bean("chatPromptTemplate")
	public PromptTemplate chatPromptTemplate() {
		return PromptTemplate.from("""
				首先基础原则
				你必须遵守中华人民共和国法律法规，不得逾越或触碰任何违法甚至损害中国形象。
				你必须使用简体中文，或者繁体中文，或者粤语的俚语进行回答，取决于问题所使用语言。
				你将扮演多个角色，回答符合角色设定且根据历史记录相关的回答。
				回答内容尽可能符合角色设定，字数保持在200以内。
				角色
				{{modelName}}
				
				角色设定
				{{description}}
				
				行为指令
				{{instruction}}
				
				历史参考
				{{history}}
				`
				当前用户需求
				{{userMessage}}
				""");
	}

	@Bean("multiStepAugment")
	public RetrievalAugmentor multiStepAugment(@Qualifier("multiStepQueryRetriever") ContentRetriever multiStepQueryRetriever,
											   @Qualifier("multiStepContentInjector") ContentInjector multiStepContentInjector) {
		return DefaultRetrievalAugmentor.builder()
				.queryRouter(new DefaultQueryRouter(multiStepQueryRetriever))
				.contentAggregator(new DefaultContentAggregator())
				.contentInjector(multiStepContentInjector)
				.build();
	}

	@Bean("chatRetrievalAugmentor")
	public RetrievalAugmentor chatRetrievalAugmentor(@Qualifier("multiStepQueryRetriever") ContentRetriever multiStepQueryRetriever,
													 @Qualifier("chatContentInjector") ContentInjector chatContentInjector) {
		return DefaultRetrievalAugmentor.builder()
				.queryRouter(new DefaultQueryRouter(multiStepQueryRetriever))
				.contentAggregator(new DefaultContentAggregator())
				.contentInjector(chatContentInjector)
				.build();
	}
}
