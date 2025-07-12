package com.shuanglin.bot.langchain4j.config.vo.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmbeddingModelProperties {
    private String baseUrl;
    private String apiKey;
    private String modelName;
}
