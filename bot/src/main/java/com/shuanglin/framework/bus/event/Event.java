package com.shuanglin.framework.bus.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.gson.annotations.SerializedName;
import com.shuanglin.framework.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
// 关键注解：用于处理多态类型
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,       // 使用一个逻辑名称来标识类型
		include = JsonTypeInfo.As.PROPERTY, // 将类型信息作为一个JSON属性包含进去
		property = "post_type_detail"     // 这个属性的名称，可以自定义，例如 "type", "event_class" 等
)
@JsonSubTypes({
		// 在这里列出所有可能的具体子类
		@JsonSubTypes.Type(value = GroupMessageEvent.class, name = "groupMessageEvent"),
		// 如果您还有其他事件，例如私聊消息事件，也在这里添加
		// @JsonSubTypes.Type(value = PrivateMessageEvent.class, name = "private_message")
})
public class Event  implements Serializable{
	@SerializedName("time")
	Long time; //事件发生的时间戳

	@SerializedName("self_id")
	Long selfId; //收到事件的机器人 QQ 号

	@SerializedName("post_type")
	EventType postType; //事件类型
}
