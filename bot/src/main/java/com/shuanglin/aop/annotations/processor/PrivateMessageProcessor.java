package com.shuanglin.aop.annotations.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuanglin.aop.BusinessLogicDispatcher;
import com.shuanglin.event.GroupEventMessage;
import com.shuanglin.aop.event.PrivateEventMessage;
import org.springframework.stereotype.Component;

@Component
public class PrivateMessageProcessor implements IMessagePreprocessor {

	private final ObjectMapper objectMapper;
	private final BusinessLogicDispatcher dispatcher;

	public PrivateMessageProcessor(ObjectMapper objectMapper, BusinessLogicDispatcher dispatcher) {
		this.objectMapper = objectMapper;
		this.dispatcher = dispatcher;
	}

	@Override
	public void process(String rawMessage) {
		try {
			PrivateEventMessage privateMessage = objectMapper.readValue(rawMessage, PrivateEventMessage.class);
			dispatcher.dispatch(privateMessage);
		} catch (Exception e) {
			System.err.println("Failed to preprocess group message.");
			e.printStackTrace();
		}
	}

	@Override
	public String getSupportedType() {
		return "private";
	}
}