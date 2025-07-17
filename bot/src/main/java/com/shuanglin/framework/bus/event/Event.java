package com.shuanglin.framework.bus.event;

import com.shuanglin.framework.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
	Long time; //事件发生的时间戳
	Long selfId; //收到事件的机器人 QQ 号
	EventType postType; //事件类型
}
