package com.shuanglin.executor.vo;

import com.shuanglin.dao.SenderInfo;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatParam {
	private GroupMessageEvent groupMessageEvent;
	private SenderInfo senderInfo;
}
