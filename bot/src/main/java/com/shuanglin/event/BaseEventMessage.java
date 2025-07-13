package com.shuanglin.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "message_type" // 使用 "message_type" 字段来区分不同的消息事件
)
@JsonSubTypes({
		// 告诉 Jackson，当 "message_type" 的值是 "group" 时，应该反序列化为 GroupEventMessage.class
		@JsonSubTypes.Type(value = GroupEventMessage.class, name = "group"),
		// 如果还有其他类型的消息，也在这里注册
		// @JsonSubTypes.Type(value = PrivateEventMessage.class, name = "private"),
		// ...
})
public abstract class BaseEventMessage {
	// 基类可以包含所有事件共有的字段，比如 post_type
	@JsonProperty("message_type")
	String messageType;
	// 你可以定义一个抽象方法，让子类实现
	public abstract String getMessageType();
}