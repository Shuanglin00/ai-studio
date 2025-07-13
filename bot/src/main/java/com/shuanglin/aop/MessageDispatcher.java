package com.shuanglin.aop;

import com.shuanglin.aop.annotations.handler.GroupMessageHandler;
import com.shuanglin.aop.annotations.handler.PrivateMessageHandler;
import com.shuanglin.aop.vo.BaseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageDispatcher {

	// 1. 【修正】注入Spring的核心ApplicationContext
	@Autowired
	private ApplicationContext applicationContext;

	// 2. 【优化】使用Map缓存，key是消息类型，value是处理器信息。这是提升性能的关键！
	// 使用ConcurrentHashMap保证线程安全
	private final Map<String, MethodHandler> handlerMap = new ConcurrentHashMap<>();

	/**
	 * 3. 【优化】使用 @PostConstruct 注解，在服务启动时执行一次，完成所有处理器的扫描和注册
	 */
	@PostConstruct
	public void initializeHandlers() {
		// 4. 【优化】扫描所有Spring容器中的Bean，而不是写死某一个
		// 你可以扫描所有@Component, @Service等，或者定义一个特定的注解如 @MessageService 来缩小范围
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);

		for (Object beanInstance : beans.values()) {
			for (Method method : beanInstance.getClass().getMethods()) {
				if (method.isAnnotationPresent(PrivateMessageHandler.class)) {
					handlerMap.put("private", new MethodHandler(beanInstance, method));
				} else if (method.isAnnotationPresent(GroupMessageHandler.class)) {
					handlerMap.put("group", new MethodHandler(beanInstance, method));
				}
			}
		}
	}

	/**
	 * 5. 【优化】分发方法接收一个已经反序列化好的对象，而不是原始字符串
	 */
	public void dispatch(BaseMessage message) {
		String messageType = message.getMessageType(); // 从消息对象中获取类型
		MethodHandler handler = handlerMap.get(messageType);

		if (handler != null) {
			try {
				// 直接将类型安全的对象传入方法
				handler.getMethod().invoke(handler.getBean(), message);
			} catch (Exception e) {
				System.err.println("Error invoking handler for message type: " + messageType);
				e.printStackTrace();
			}
		} else {
			System.err.println("No handler found for message type: " + messageType);
		}
	}

	/**
	 * 内部类，用于封装一个处理器方法及其所属的Bean实例
	 */
	private static class MethodHandler {
		private final Object bean;
		private final Method method;

		public MethodHandler(Object bean, Method method) {
			this.bean = bean;
			this.method = method;
		}

		// getters...
		public Object getBean() { return bean; }
		public Method getMethod() { return method; }

		@Override
		public String toString() {
			return "MethodHandler{" + "bean=" + bean.getClass().getSimpleName() + ", method=" + method.getName() + '}';
		}
	}
}