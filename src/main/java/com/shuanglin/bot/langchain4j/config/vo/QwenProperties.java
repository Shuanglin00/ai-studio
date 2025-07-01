package com.shuanglin.bot.langchain4j.config.vo;

import com.shuanglin.bot.langchain4j.config.vo.qwen.QwenApiProperty;
import com.shuanglin.bot.langchain4j.config.vo.qwen.QwenEmbeddingModelProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
@ConfigurationProperties(prefix = QwenProperties.PREFIX)
public class QwenProperties {
	public static final String PREFIX = "langchain4j.models.qwen";
//
//	@NestedConfigurationProperty
//	QwenApiProperty apiModel;

	@NestedConfigurationProperty
	QwenEmbeddingModelProperty embeddingModel;
}
