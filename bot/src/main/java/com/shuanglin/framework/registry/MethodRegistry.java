package com.shuanglin.framework.registry;

import com.shuanglin.framework.annotation.GroupMessageHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MethodRegistry {

	private final ApplicationContext applicationContext;

	@Getter
	private final List<MethodInfo> groupMessageHandlers = new ArrayList<>();

	// 监听 Spring 容器刷新完成事件，此时所有 Bean 都已初始化
	@EventListener(ContextRefreshedEvent.class)
	public void onApplicationEvent() {
		log.info("Framework startup: Scanning for message handlers...");
		scanGroupMessageHandlers();
		// 如果有其他类型的处理器，在这里添加扫描方法
		log.info("Framework startup: Scan complete. Found {} group message handlers.", groupMessageHandlers.size());
	}

	private void scanGroupMessageHandlers() {
		// 获取所有被 @GroupMessageHandler 注解的方法所在的 Bean
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class); // 扫描所有组件
		for (Object bean : beans.values()) {
			// Spring 会创建代理类，要获取原始类来检查方法
			Class<?> targetClass = AopUtils.getTargetClass(bean);
			for (Method method : targetClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(GroupMessageHandler.class)) {
					GroupMessageHandler annotation = method.getAnnotation(GroupMessageHandler.class);
					// 确保方法只有一个参数，即 payload
					if (method.getParameterCount() != 1) {
						log.error("Method {} annotated with @GroupMessageHandler must have exactly one parameter (the payload).", method.getName());
						continue;
					}
					MethodInfo methodInfo = new MethodInfo(bean, method, annotation);
					groupMessageHandlers.add(methodInfo);
					log.info("Registered handler: {}.{}", targetClass.getSimpleName(), method.getName());
				}
			}
		}
	}
}