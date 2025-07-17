package com.shuanglin.framework.bus.event;

import com.shuanglin.framework.bus.event.data.Sender;
import com.shuanglin.framework.enums.MessageType.MessageTypeEnum;
import com.shuanglin.framework.enums.MessageType.SubMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageEvent extends Event{
	MessageTypeEnum messageType;

	SubMessageTypeEnum subType;

	Long messageId;

	Long userId;

	Object message;

	Object rawMessage;

	Long font;

	Sender sender;
}
