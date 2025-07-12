package com.shuanglin.aop.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Jackson的注解保持不变，它能很好地配合Builder模式工作
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "messageType"
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = PrivateMessage.class, name = "private"),
		@JsonSubTypes.Type(value = GroupMessage.class, name = "group")
})
public abstract class BaseMessage {

	// 属性变为 final，确保不可变性
	protected final String messageType;

	// 构造函数是 protected，只能被子类或其内部的Builder调用
	protected BaseMessage(Builder<?> builder) {
		this.messageType = builder.messageType;
	}

	public String getMessageType() {
		return messageType;
	}

	// 泛型化的抽象Builder
	// B extends Builder<B> 是一种自限定泛型，确保链式调用返回的是子类Builder本身
	public abstract static class Builder<B extends Builder<B>> {
		private String messageType;

		// 子类Builder会调用这个方法
		protected B self() {
			return (B) this;
		}

		public B messageType(String messageType) {
			this.messageType = messageType;
			return self();
		}

		// 抽象的build方法，由子类实现
		public abstract BaseMessage build();
	}
}