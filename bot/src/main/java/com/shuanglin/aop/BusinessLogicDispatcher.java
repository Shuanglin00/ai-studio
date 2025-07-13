package com.shuanglin.aop;// --- File: com/shuanglin/framework/core/BusinessLogicDispatcher.java ---
// ... imports ...
import com.shuanglin.aop.annotations.handler.GroupMessageHandler;
import com.shuanglin.aop.annotations.handler.PrivateMessageHandler;
import com.shuanglin.aop.vo.BaseMessage;
import com.shuanglin.aop.vo.HandlerMethodInfo;
import com.shuanglin.event.BaseEventMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component("businessLogicDispatcher")
public class BusinessLogicDispatcher {

	private final Map<String, List<HandlerMethodInfo>> handlerMap = new ConcurrentHashMap<>();

	private final ApplicationContext applicationContext;

	// 1. 构造函数现在非常干净，只负责接收依赖，不做任何复杂操作
	public BusinessLogicDispatcher(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	// 2. 将所有初始化逻辑移到这个方法中
	//    这个方法会在 BusinessLogicDispatcher 实例被完全创建后才执行
	@PostConstruct
	public void initializeHandlers() {
		System.out.println("Executing @PostConstruct: Initializing business logic handlers...");
		// 扫描所有Bean
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
		for (Object bean : beans.values()) {
			for (Method method : bean.getClass().getDeclaredMethods()) {
				HandlerMethodInfo handlerInfo = new HandlerMethodInfo(bean, method);
				if (method.isAnnotationPresent(GroupMessageHandler.class)) {
					handlerMap.computeIfAbsent("group", k -> new CopyOnWriteArrayList<>()).add(handlerInfo);
				} else if (method.isAnnotationPresent(PrivateMessageHandler.class)) {
					handlerMap.computeIfAbsent("private", k -> new CopyOnWriteArrayList<>()).add(handlerInfo);
				}
			}
		}
		System.out.println("Business Logic Handlers Initialized: " + handlerMap);
	}

	public void dispatch(BaseEventMessage message) {
		String messageType = "group";
		List<HandlerMethodInfo> handlers = handlerMap.get(messageType);
		if (handlers != null && !handlers.isEmpty()) {
			for (HandlerMethodInfo handler : handlers) {
				try {
					handler.getMethod().invoke(handler.getBeanInstance(), message);
				} catch (Exception e) {
					System.err.println("Error invoking business handler: " + handler);
					e.printStackTrace();
				}
			}
		}
	}
}