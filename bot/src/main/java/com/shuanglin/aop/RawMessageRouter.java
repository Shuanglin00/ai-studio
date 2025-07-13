package com.shuanglin.aop;

// ... imports ...
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuanglin.aop.annotations.processor.IMessagePreprocessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RawMessageRouter {

	private final ObjectMapper objectMapper;
	private final Map<String, IMessagePreprocessor> preprocessorMap = new ConcurrentHashMap<>();

	// Spring会自动将所有IMessagePreprocessor的实现注入到这个List中
	public RawMessageRouter(ObjectMapper objectMapper, List<IMessagePreprocessor> preprocessors) {
		this.objectMapper = objectMapper;
		// 将List转换为Map，便于快速查找
		for (IMessagePreprocessor p : preprocessors) {
			this.preprocessorMap.put(p.getSupportedType(), p);
		}
		System.out.println("Preprocessors Registered: " + preprocessorMap.keySet());
	}

	/**
	 * 框架总入口
	 */
	public void route(String rawJsonMessage) {
		try {
			JsonNode rootNode = objectMapper.readTree(rawJsonMessage);
			String messageType = rootNode.path("message_type").asText("unknown"); // 假设类型字段是"message_type"

			IMessagePreprocessor preprocessor = preprocessorMap.get(messageType);
			if (preprocessor != null) {
				preprocessor.process(rawJsonMessage);
			} else {
				System.err.println("No preprocessor found for message type: " + messageType);
			}
		} catch (Exception e) {
			System.err.println("Invalid raw message format.");
			e.printStackTrace();
		}
	}
}