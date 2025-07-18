package com.shuanglin.framework.bus.event;

import com.google.gson.annotations.SerializedName;
import com.shuanglin.framework.bus.event.data.Sender;
import com.shuanglin.framework.enums.MessageType.MessageTypeEnum;
import com.shuanglin.framework.enums.MessageType.SubMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageEvent extends Event implements Serializable {
	@SerializedName("message_type")
	MessageTypeEnum messageType;

	@SerializedName("sub_type")
	SubMessageTypeEnum subType;

	@SerializedName("message_id")
	Long messageId;

	@SerializedName("user_id")
	Long userId;

	@SerializedName("message")
	String message;

	@SerializedName("raw_message")
	String rawMessage;

	@SerializedName("font")
	Long font;

	@SerializedName("sender")
	Sender sender;
}
