package com.shuanglin.bot.langchain4j.config.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalLLMModel {

    @Bean
    OpenAiChatModel localLLMModel() {
        return OpenAiChatModel.builder()
                .baseUrl("http://localhost:1234/v1/chat/completions")
                .modelName("Local-Gemma3")
                .build();
    }

}
