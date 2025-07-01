package com.shuanglin.bot.langchain4j.config.vo;

import com.shuanglin.bot.langchain4j.config.vo.gemini.GeminiApiProperty;
import com.shuanglin.bot.langchain4j.config.vo.gemini.GeminiEmbeddingModelProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = GeminiProperties.PREFIX)
public class GeminiProperties {
	public static final String PREFIX = "langchain4j.models.gemini";

	@NestedConfigurationProperty
	GeminiApiProperty apiModel;

	@NestedConfigurationProperty
	GeminiEmbeddingModelProperty embeddingModel;
}
