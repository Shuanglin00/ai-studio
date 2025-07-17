package com.shuanglin.framework.bus;

import com.shuanglin.framework.bus.event.Event;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 全局消息总线，单例。
 * 负责接收和广播所有消息。
 */
@Component
public class MessageBus {

	// 使用 Reactor Sinks 作为响应式流的源
	// many().multicast() 允许多个订阅者
	private final Sinks.Many<Event> sink = Sinks.many().multicast().onBackpressureBuffer();

	/**
	 * 向总线发布一条消息。
	 *
	 * @param message 要发布的消息
	 */
	public void publish(Event event) {
		sink.tryEmitNext(event);
	}

	/**
	 * 获取总线的 Flux 流，供订阅者使用。
	 *
	 * @return 返回一个可订阅的消息流
	 */
	public Flux<Event> getBus() {
		return sink.asFlux();
	}
}