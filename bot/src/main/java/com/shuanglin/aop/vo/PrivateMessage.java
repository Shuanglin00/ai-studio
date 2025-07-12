package com.shuanglin.aop.vo;

public class PrivateMessage extends BaseMessage {

	// 属性全部设为final
	private final Long fromUserId;
	private final Long toUserId;
	private final String content;

	// 私有构造函数，强制使用Builder来创建实例
	private PrivateMessage(Builder builder) {
		super(builder); // 调用父类的构造函数
		this.fromUserId = builder.fromUserId;
		this.toUserId = builder.toUserId;
		this.content = builder.content;
	}

	// Getters
	public Long getFromUserId() { return fromUserId; }
	public Long getToUserId() { return toUserId; }
	public String getContent() { return content; }

	// 提供一个静态方法作为Builder的入口，这是标准做法
	public static Builder builder() {
		return new Builder();
	}

	// 静态内部类Builder
	public static final class Builder extends BaseMessage.Builder<Builder> {
		private Long fromUserId;
		private Long toUserId;
		private String content;

		// 构造时就设定好消息类型
		private Builder() {
			super.messageType("private");
		}

		public Builder fromUserId(Long fromUserId) {
			this.fromUserId = fromUserId;
			return this; // 返回自身，实现链式调用
		}

		public Builder toUserId(Long toUserId) {
			this.toUserId = toUserId;
			return this;
		}

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		// self()方法在父类中已经实现，这里无需重复

		@Override
		public PrivateMessage build() {
			// 在build时进行参数校验
			// if (fromUserId == null || toUserId == null) {
			//     throw new IllegalStateException("fromUserId and toUserId cannot be null");
			// }
			return new PrivateMessage(this);
		}
	}
}