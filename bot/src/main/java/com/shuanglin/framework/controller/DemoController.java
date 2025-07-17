package com.shuanglin.framework.controller;

import com.shuanglin.framework.annotation.PublishBus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	/**
	 * {"self_id":2784152733,"user_id":1751649231,"time":1752765710,"message_id":521646554,"message_seq":23600,"message_type":"group","sender":{"user_id":1751649231,"nickname":"双零","card":"","role":"owner","title":""},"raw_message":"123","font":14,"sub_type":"normal","message":"123","message_format":"string","post_type":"message","group_id":345693826}
	 *{"self_id":2784152733,"user_id":1751649231,"time":1752765752,"message_id":2014142285,"message_seq":2442,"message_type":"private","sender":{"user_id":1751649231,"nickname":"双零","card":""},"raw_message":"123","font":14,"sub_type":"friend","message":"123","message_format":"string","post_type":"message"}
	 * @param payload
	 * @return
	 */
	@PostMapping("/bot")
	@PublishBus(type = "group") // 声明此方法的返回值将作为 "group" 类型消息发布
	public String sendGroupMessage(@RequestBody String payload) {
		System.out.println("payload = " + payload);
		// Controller 只负责接收HTTP请求和返回数据，不关心后续处理
		// 返回的 Map 对象将成为消息的 payload
		return payload;
	}
}