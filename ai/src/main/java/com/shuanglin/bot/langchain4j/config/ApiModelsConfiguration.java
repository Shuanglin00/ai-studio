package com.shuanglin.bot.langchain4j.config;

import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import com.shuanglin.bot.langchain4j.assistant.OllamaAssistant;
import com.shuanglin.bot.langchain4j.config.rag.NonMemoryRetriever;
import com.shuanglin.bot.langchain4j.config.store.FilterMemoryStore;
import com.shuanglin.bot.langchain4j.config.store.NonMemoryStore;
import com.shuanglin.bot.langchain4j.config.vo.GeminiProperties;
import com.shuanglin.bot.langchain4j.config.vo.QwenProperties;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 模型bean管理
 *
 * @author lin
 * @date 2025/06/27
 */
@Configuration
@EnableConfigurationProperties({GeminiProperties.class, QwenProperties.class})
public class ApiModelsConfiguration {


	@Bean
	public OllamaChatModel chatLanguageModel() {
		return OllamaChatModel.builder()
				.baseUrl("http://localhost:11434")
				.temperature(0.0) // 模型温度，控制模型生成的随机性，0-1之间，越大越多样性
				.logRequests(true)
				.logRequests(true)
				.logResponses(true)
				.modelName("gemma3:1b")
				.build();
	}
	@Bean
	public OllamaStreamingChatModel chatStreamingLanguageModel() {
		return OllamaStreamingChatModel.builder()
				.baseUrl("http://localhost:11434")
				.temperature(0.0) // 模型温度，控制模型生成的随机性，0-1之间，越大越多样性
				.logRequests(true)
				.logRequests(true)
				.logResponses(true)
				.modelName("gemma3:1b")
				.build();
	}

	@Bean
	public OllamaAssistant ollamaAssistant(OllamaChatModel ollamaChatModel,
										   OllamaStreamingChatModel chatStreamingLanguageModel,
										   NonMemoryStore nonMemoryStore,
										   NonMemoryRetriever nonMemoryRetriever
//	                                       RetrievalAugmentor retrievalAugmentor

	) {

		return AiServices.builder(OllamaAssistant.class)
				.chatModel(ollamaChatModel)
				.streamingChatModel(chatStreamingLanguageModel)
				.chatMemoryProvider(modelId -> MessageWindowChatMemory.builder()
						.id(modelId)
						.maxMessages(10)
						.chatMemoryStore(nonMemoryStore)
						.build())
				.contentRetriever(nonMemoryRetriever)
				.build();
	}

	@Bean
	GoogleAiGeminiChatModel googleAiGeminiChatModel(GeminiProperties geminiProperties,List<ChatModelListener> chatModelListenerList) {
		return GoogleAiGeminiChatModel.builder()
				.apiKey(geminiProperties.getApiKey())
				.modelName(geminiProperties.getModelName())
				.temperature(geminiProperties.getTemperature())
				.topP(geminiProperties.getTopP())
				.topK(geminiProperties.getTopK())
				.listeners(chatModelListenerList)
				.build();
	}
	@Bean
	GoogleAiGeminiStreamingChatModel googleAiGeminiStreamingChatModel(GeminiProperties geminiProperties, List<ChatModelListener> chatModelListenerList) {
		return GoogleAiGeminiStreamingChatModel.builder()
				.apiKey(geminiProperties.getApiKey())
				.modelName(geminiProperties.getModelName())
				.temperature(geminiProperties.getTemperature())
				.topP(geminiProperties.getTopP())
				.topK(geminiProperties.getTopK())
				.listeners(chatModelListenerList)
				.build();
	}
	@Bean
	public GeminiAssistant geminiAssistant(GoogleAiGeminiChatModel localLLMModel,
										   GoogleAiGeminiStreamingChatModel googleAiGeminiStreamingChatModel,
										   FilterMemoryStore filterMemoryStore,
										   RetrievalAugmentor chatRetrievalAugmentor

	) {

		return AiServices.builder(GeminiAssistant.class)
				.chatModel(localLLMModel)
				.streamingChatModel(googleAiGeminiStreamingChatModel)
				.chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
						.id(memoryId)
						.maxMessages(10)
						.chatMemoryStore(filterMemoryStore)
						.build())
				.retrievalAugmentor(chatRetrievalAugmentor)
				.build();
	}


}
