package com.shuanglin.aop.annotations.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.shuanglin.aop.BusinessLogicDispatcher;
import com.shuanglin.aop.vo.GroupMessage;
import com.shuanglin.event.GroupEventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroupMessagePreprocessor implements IMessagePreprocessor {
	private ObjectMapper objectMapper;
	private BusinessLogicDispatcher dispatcher;

	@Autowired
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Autowired
	public void setDispatcher(BusinessLogicDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public void process(String rawMessage) {
		try {
			GroupEventMessage groupMessage = objectMapper.readValue(rawMessage, GroupEventMessage.class);
			dispatcher.dispatch(groupMessage);
		} catch (Exception e) {
			System.err.println("Failed to preprocess group message.");
			e.printStackTrace();
		}
	}

	@Override
	public String getSupportedType() {
		return "group";
	}
}