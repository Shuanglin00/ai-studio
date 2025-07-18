package com.shuanglin.framework.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MessageType {
	@NoArgsConstructor
	@Getter
	@AllArgsConstructor
	public enum MessageTypeEnum {
		/**
		 * 消息类型枚举
		 */
		groupMessage("group"),
		privateMessage("private");
		String value;
	}

	@NoArgsConstructor
	@Getter
	@AllArgsConstructor
	public enum SubMessageTypeEnum {
		/**
		 * 消息子类型，如果是好友则是 friend，如果是群临时会话则是 group
		 */
		friend("friend"),
		group("group"),
		normal("normal"),
		other("other");
		String value;
	}
}
