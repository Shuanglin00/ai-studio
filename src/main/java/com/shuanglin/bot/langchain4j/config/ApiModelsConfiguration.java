package com.shuanglin.bot.langchain4j.config;

import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import com.shuanglin.bot.langchain4j.config.vo.GeminiProperties;
import com.shuanglin.bot.langchain4j.config.vo.gemini.GeminiApiProperty;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

/**
 * 模型bean管理
 *
 * @author lin
 * @date 2025/06/27
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class ApiModelsConfiguration {

	@Bean
	GoogleAiGeminiChatModel googleAiGeminiChatModel(GeminiProperties geminiProperties,List<ChatModelListener> chatModelListenerList) {
		GeminiApiProperty apiModel = geminiProperties.getApiModel();
		return GoogleAiGeminiChatModel.builder()
				.apiKey(apiModel.getApiKey())
				.modelName(apiModel.getModelName())
				.temperature(apiModel.getTemperature())
				.topP(apiModel.getTopP())
				.topK(apiModel.getTopK())
				.listeners(chatModelListenerList)
				.build();
	}
	@Bean
	GoogleAiGeminiStreamingChatModel googleAiGeminiStreamingChatModel(GeminiProperties geminiProperties, List<ChatModelListener> chatModelListenerList) {
		GeminiApiProperty apiModel = geminiProperties.getApiModel();
		return GoogleAiGeminiStreamingChatModel.builder()
				.apiKey(apiModel.getApiKey())
				.modelName(apiModel.getModelName())
				.temperature(apiModel.getTemperature())
				.topP(apiModel.getTopP())
				.topK(apiModel.getTopK())
				.listeners(chatModelListenerList)
				.build();
	}
	@Bean
	public GeminiAssistant geminiAssistant(GoogleAiGeminiChatModel googleAiGeminiChatModel,
	                                   GoogleAiGeminiStreamingChatModel googleAiGeminiStreamingChatModel,
										   RedisMemoryStore redisMemoryStore) {

		return AiServices.builder(GeminiAssistant.class)
				.chatModel(googleAiGeminiChatModel)
				.streamingChatModel(googleAiGeminiStreamingChatModel)
				.chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
						.id(memoryId)
						.maxMessages(10)
						.chatMemoryStore(redisMemoryStore)
						.build())
				.build();
	}


}
