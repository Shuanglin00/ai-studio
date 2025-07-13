package com.shuanglin.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shuanglin.event.vo.Sender;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrivateEventMessage extends BaseEventMessage {

	@JsonProperty("time")
	private long time;

	@JsonProperty("self_id")
	private long selfId; // 机器人自己的QQ号

	@JsonProperty("post_type")
	private String postType;

	@JsonProperty("message_type")
	private String messageType;

	@JsonProperty("sub_type")
	private String subType;

	@JsonProperty("message_id")
	private int messageId;

	@JsonProperty("group_id")
	private long groupId;

	@JsonProperty("user_id")
	private long userId; // 发送者QQ号

	@JsonProperty("sender")
	private Sender sender;

	// 关键字段：消息内容。它可以是String或Array，这里先用String接收简单情况
	@JsonProperty("message")
	private String message;

	@JsonProperty("raw_message")
	private String rawMessage;

	@Override
	public String getMessageType() {
		// 让它可以被我们现有的框架识别
		return this.messageType;
	}
}