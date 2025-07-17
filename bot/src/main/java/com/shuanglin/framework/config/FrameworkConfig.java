package com.shuanglin.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

@Configuration
public class FrameworkConfig {

	@Bean
	public ExpressionParser expressionParser() {
		return new SpelExpressionParser();
	}
}