package com.shuanglin.bot.config;

import lombok.Data;

@Data
public class DBMessageDTO {
	private String id; // MongoDB自动生成
	private String memoryId; // 会话ID
	private String userId; // 用户ID
	private String groupId; // 用户ID
	private String role; // 那个模型 聊天的 代码的
	private String content; // 消息内容
	private Long lastChatTime; // 消息时间戳
	// toolExecutionRequests, name等其它字段可扩展
}
