package com.shuanglin.bot.langchain4j.config;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PromptTemplateConfig {
	@Bean
	public PromptTemplate promptTemplate() {
		return PromptTemplate.from(
				"""
				{{history}}
	
				综合以下相关信息回答问题
				--------------------------
				检索到的信息
				{{contents}}
	
				用户问题：{{userMessage}}
				"""
		);
	}

	@Bean
	public DefaultContentInjector defaultContentInjector(PromptTemplate promptTemplate) {
		return DefaultContentInjector.builder().promptTemplate(promptTemplate).build();
	}
}
