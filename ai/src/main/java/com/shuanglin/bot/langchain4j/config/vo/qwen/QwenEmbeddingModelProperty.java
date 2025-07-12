package com.shuanglin.bot.langchain4j.config.vo.qwen;

import com.shuanglin.bot.langchain4j.config.vo.base.EmbeddingModelProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QwenEmbeddingModelProperty extends EmbeddingModelProperties {
	String projectId;
	String location;
	String modelName;
}
