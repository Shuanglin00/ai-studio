//package com.shuanglin.framework.listener;
//
//import com.google.gson.Gson;
//import com.shuanglin.framework.bus.MessageBus;
//import com.shuanglin.framework.registry.MethodInvoker;
//import com.shuanglin.framework.registry.MethodRegistry;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import reactor.core.scheduler.Schedulers;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PrivateMessageListener {
//
//	private final MessageBus messageBus;
//	private final MethodRegistry methodRegistry;
//	private final MethodInvoker methodInvoker;
//
//	// 在依赖注入完成后，开始订阅
//	@PostConstruct
//	public void subscribe() {
//		log.info("GroupMessageListener is subscribing to the message bus for 'group' type messages.");
//		messageBus.getBus()
//				// 1. 过滤出自己关心的消息类型
//				.filter(event -> event.get("post_type").getAsString().equals("message") && event.get("message_type").getAsString().equals("group"))
//				// 2. 在独立的线程池中异步处理，避免阻塞总线
//				.publishOn(Schedulers.boundedElastic())
//				.doOnNext(event -> {
//					// 3. 处理消息
//					processMessage(new Gson().fromJson(event, Pr.class));
//				})
//				.subscribe(
//						null, // 成功处理时无需额外操作
//						error -> log.error("Error in GroupMessageListener stream", error) // 处理流中的错误
//				);
//	}
//
//	private void processMessage(GroupMessageEvent event) {
//		log.info("Listener received a 'group' message. event: {}", event);
//		// 找到所有注册的 GroupMessageHandler
//		methodRegistry.getGroupMessageHandlers().forEach(methodInfo -> {
//			// 通过 MethodInvoker 调用，这将触发 MessageHandlerAspect
//			methodInvoker.invoke(methodInfo, event);
//		});
//	}
//}