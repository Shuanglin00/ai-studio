package com.shuanglin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {
	/** 群组消息 */
	GROUP,
	/** 私聊消息 */
	PRIVATE,
	/** 好友相关消息 (例如：好友请求, 好友通知) */
	FRIEND,
	/** 系统消息 (例如：系统通知, 广播) */
	SYSTEM,
	/** 其他未知或不匹配的类型 */
	OTHER
}
