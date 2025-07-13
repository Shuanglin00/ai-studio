package com.shuanglin.aop.annotations.processor;

public interface IMessagePreprocessor {
	/**
	 * 处理原始消息字符串
	 * @param rawMessage 原始消息
	 */
	void process(String rawMessage);

	/**
	 * 返回该处理器能处理的消息类型
	 * @return 消息类型，如 "group"
	 */
	String getSupportedType();
}
