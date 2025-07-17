package com.shuanglin.framework.command;

import com.shuanglin.framework.annotation.GroupMessageHandler;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandHandler {

	/**
	 * 这个处理器只处理 VIP 用户的消息。
	 * condition = "#payload['vip'] == true" 使用了 SpEL 表达式。
	 * 它会检查传入的 Map payload 中是否有一个 key 为 'vip' 且值为 true 的条目。
	 */
	@GroupMessageHandler()
	public void handleVipGroupMessage(GroupMessageEvent event) {
		log.info("✅ [VIP HANDLER] Received a message for VIP user: {}", event);
		// 在这里执行针对 VIP 用户的特定业务逻辑...
	}
}