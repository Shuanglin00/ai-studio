package com.shuanglin.bot.langchain4j.config.vo;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GeminiFunctionCallingConfig;
import dev.langchain4j.model.googleai.GeminiSafetySetting;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = GeminiProperties.PREFIX)
public class GeminiProperties {
	public static final String PREFIX = "langchain4j.models.gemini";

	String apiKey;
	String modelName;
	Integer maxRetries;
	Double temperature;
	Integer topK;
	Integer seed;
	Double topP;
	Integer maxOutputTokens;
	Duration timeout;
	ResponseFormat responseFormat;
	List<String> stopSequences;
	GeminiFunctionCallingConfig toolConfig;
	Boolean allowCodeExecution;
	Boolean includeCodeExecutionOutput;
	Boolean logRequestsAndResponses;
	List<GeminiSafetySetting> safetySettings;
	List<ChatModelListener> listeners;
}
