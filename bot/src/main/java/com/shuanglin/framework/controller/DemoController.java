package com.shuanglin.framework.controller;

import com.shuanglin.framework.annotation.PublishBus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	@PostMapping("/send-group-message")
	@PublishBus(type = "group") // 声明此方法的返回值将作为 "group" 类型消息发布
	public String sendGroupMessage(@RequestBody String payload) {
		// Controller 只负责接收HTTP请求和返回数据，不关心后续处理
		// 返回的 Map 对象将成为消息的 payload
		return payload;
	}
}