package com.shuanglin.bot.langchain4j.rag.config;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class PromptTempleConfig {

	@Bean("chatPromptTemplate")
	public PromptTemplate chatPromptTemplate() {
		return PromptTemplate.from("# 首先基础原则\n" +
				"1. 你必须遵守中华人民共和国法律法规，不得逾越或触碰任何违法甚至损害中国形象。\n" +
				"2. 你必须使用简体中文，或者繁体中文，或者粤语的俚语进行回去，取决于问题所使用语言。\n" +
				"3. 你将扮演多个角色，回答符合角色设定且根据历史记录相关的回答。\n" +
				"4. 回答内容尽可能符合角色设定，字数保持在200以内。\n" +
				"---\n" +
				"# 角色\n" +
				"{{role}}\n" +
				"# 角色设定\n" +
				"{{description}}\n" +
				"\n" +
				"--- \n" +
				"# 行为指令\n" +
				"{{instruction}}\n" +
				"\n" +
				"---\n" +
				"\n" +
				"---\n" +
				"# 当前用户需求\n" +
				"{{userMessage}}");
	}

	@Bean("chatRetrievalAugmentor")
	public RetrievalAugmentor chatRetrievalAugmentor(@Qualifier("NonMemoryRetriever") ContentRetriever NonMemoryRetriever) {
		return DefaultRetrievalAugmentor.builder()
				.queryRouter(new DefaultQueryRouter(NonMemoryRetriever))
				.contentAggregator(new DefaultContentAggregator())
				.contentInjector(
						DefaultContentInjector.builder()
								.promptTemplate(chatPromptTemplate())

								.build())
				.build();
	}
}
