package com.shuanglin.bot.langchain4j.config;

import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import com.shuanglin.bot.langchain4j.assistant.OllamaAssistant;
import com.shuanglin.bot.langchain4j.store.FilterMemoryStore;
import com.shuanglin.bot.langchain4j.config.vo.GeminiProperties;
import com.shuanglin.bot.langchain4j.config.vo.OllamaProperties;
import com.shuanglin.bot.langchain4j.config.vo.QwenProperties;
import com.shuanglin.bot.langchain4j.tools.DocumentInsertTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;

/**
 * 模型bean管理
 *
 * @author lin
 * @date 2025/06/27
 */
@Configuration
@EnableConfigurationProperties({GeminiProperties.class, QwenProperties.class, OllamaProperties.class})
public class ApiModelsConfiguration {
	@Resource
	private DocumentInsertTool documentInsertTool;

	@Bean("decomposeLanguageModel")
	public OllamaChatModel decomposeLanguageModel(OllamaProperties ollamaProperties) {
		return OllamaChatModel.builder()
				.baseUrl(ollamaProperties.getUrl())
				.temperature(ollamaProperties.getTemperature()) // 模型温度，控制模型生成的随机性，0-1之间，越大越多样性
				.logRequests(true)
				.logResponses(true)
				.modelName("gemma3:1b")
				.build();
	}

	@Bean("chatLanguageModel")
	public OllamaChatModel chatLanguageModel(OllamaProperties ollamaProperties) {
		return OllamaChatModel.builder()
				.baseUrl(ollamaProperties.getUrl())
				.temperature(ollamaProperties.getTemperature()) // 模型温度，控制模型生成的随机性，0-1之间，越大越多样性
				.logRequests(true)
				.logResponses(true)
				.modelName(ollamaProperties.getModelName())
				.build();
	}

	@Bean
	public OllamaStreamingChatModel chatStreamingLanguageModel(OllamaProperties ollamaProperties) {
		return OllamaStreamingChatModel.builder()
				.baseUrl(ollamaProperties.getUrl())
				.temperature(ollamaProperties.getTemperature()) // 模型温度，控制模型生成的随机性，0-1之间，越大越多样性
				.logRequests(true)
				.logResponses(true)
				.modelName(ollamaProperties.getModelName())
				.build();
	}

	@Bean("assistant_v4")
	public OllamaAssistant assistant_v4(@Qualifier("chatLanguageModel") OllamaChatModel chatLanguageModel,
										   OllamaStreamingChatModel chatStreamingLanguageModel,
										   RetrievalAugmentor chatRetrievalAugmentor) {

		return AiServices.builder(OllamaAssistant.class)
				.chatModel(chatLanguageModel)
				.streamingChatModel(chatStreamingLanguageModel)
				.retrievalAugmentor(chatRetrievalAugmentor)
				.build();
	}

	@Bean
	public OllamaAssistant ollamaAssistant(@Qualifier("chatLanguageModel") OllamaChatModel chatLanguageModel,
										   OllamaStreamingChatModel chatStreamingLanguageModel,
										   RetrievalAugmentor chatRetrievalAugmentor,
										   FilterMemoryStore filterMemoryStore) {

		return AiServices.builder(OllamaAssistant.class)
				.chatModel(chatLanguageModel)
				.streamingChatModel(chatStreamingLanguageModel)
				.chatMemoryProvider(modelId -> MessageWindowChatMemory.builder()
						.id(modelId)
						.maxMessages(10)
						.chatMemoryStore(filterMemoryStore)
						.build())
				.retrievalAugmentor(chatRetrievalAugmentor)
				.build();
	}

	@Bean
	GoogleAiGeminiChatModel googleAiGeminiChatModel(GeminiProperties geminiProperties, List<ChatModelListener> chatModelListenerList) {
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
	public GeminiAssistant geminiAssistant(GoogleAiGeminiChatModel chatModel,
										   GoogleAiGeminiStreamingChatModel googleAiGeminiStreamingChatModel,
										   FilterMemoryStore filterMemoryStore,
										   RetrievalAugmentor chatRetrievalAugmentor

	) {

		return AiServices.builder(GeminiAssistant.class)
				.chatModel(chatModel)
				.streamingChatModel(googleAiGeminiStreamingChatModel)
				.chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
						.id(memoryId)
						.maxMessages(1) //自行存储message
						.chatMemoryStore(filterMemoryStore)
						.build())
				.retrievalAugmentor(chatRetrievalAugmentor)
				.build();
	}


}
