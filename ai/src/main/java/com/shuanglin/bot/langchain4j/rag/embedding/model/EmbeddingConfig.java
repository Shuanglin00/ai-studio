package com.shuanglin.bot.langchain4j.rag.embedding.model;

import com.shuanglin.bot.langchain4j.config.vo.QwenProperties;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EmbeddingConfig {
    @Bean
    @Primary
    public EmbeddingModel qwenEmbeddingModel(QwenProperties qwenProperties) {
        return QwenEmbeddingModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(qwenProperties.getModelName())
                .build();
    }

//	@Bean
//	public EmbeddingModel embeddingModel() {
//		// 示例：用BGE small zh模型，实际按你的需求配置
//		return new BgeSmallZhEmbeddingModel();
//	}

//
//	@Bean
//	@Primary
//	public ChatModel googleEmbeddingModel(GeminiProperties geminiProperties) {
//		GeminiEmbeddingModelProperty properties = geminiProperties.getEmbeddingModel();
//		return VertexAiGeminiChatModel.builder()
//				.project(properties.getProjectId())
//				.location(properties.getLocation())
//				.modelName(properties.getModelName())
//				.build();
//	}
//	@Bean
//	@Primary
//	public StreamingChatModel googleStreamingEmbeddingModel(GeminiProperties geminiProperties) {
//		GeminiEmbeddingModelProperty properties = geminiProperties.getEmbeddingModel();
//			return VertexAiGeminiStreamingChatModel.builder()
//					.project(properties.getProjectId())
//					.location(properties.getLocation())
//					.modelName(properties.getModelName())
//					.build();
//		}

/*	ChatModel model = VertexAiGeminiChatModel.builder()
			.project(PROJECT_ID)        // your Google Cloud project ID
			.location(LOCATION)         // the region where AI inference should take place
			.modelName(MODEL_NAME)      // the model used
			.logRequests(true)          // log input requests
			.logResponses(true)         // log output responses
			.maxOutputTokens(8192)      // the maximum number of tokens to generate (up to 8192)
			.temperature(0.7)           // temperature (between 0 and 2)
			.topP(0.95)                 // topP (between 0 and 1) — cumulative probability of the most probable tokens
			.topK(3)                    // topK (positive integer) — pick a token among the most probable ones
			.seed(1234)                 // seed for the random number generator
			.maxRetries(2)              // maximum number of retries
			.responseMimeType("application/json") // to get JSON structured outputs
			.responseSchema(*//*...*//*)    // structured output following the provided schema
			.safetySettings(*//*...*//*)    // specify safety settings to filter inappropriate content
			.useGoogleSearch(true)      // to ground responses with Google Search results
			.vertexSearchDatastore(name)// to ground responses with data backed documents
			// from a custom Vertex AI Search datastore
			.toolCallingMode(*//*...*//*)   // AUTO (automatic), ANY (from a list of functions), NONE
			.allowedFunctionNames(*//*...*//*) // when using ANY tool calling mode,
			// specify the allowed function names to be called
			.listeners(*//*...*//*)         // list of listeners to receive model events
			.build();*/
}
