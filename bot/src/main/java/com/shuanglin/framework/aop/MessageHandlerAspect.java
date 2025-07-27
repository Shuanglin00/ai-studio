package com.shuanglin.framework.aop;

import com.shuanglin.framework.annotation.GroupMessageHandler;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MessageHandlerAspect {

	private final ExpressionParser expressionParser;

	// 定义切点，拦截所有 @GroupMessageHandler 注解的方法
	// 注意：如果有多种处理器注解，需要为每一种都定义切点和通知
	@Around("@annotation(com.shuanglin.framework.annotation.GroupMessageHandler)")
	public Object handleMessage(ProceedingJoinPoint pjp) throws Throwable {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		Method method = signature.getMethod();
		boolean proceed = true;
		GroupMessageHandler annotation = method.getAnnotation(GroupMessageHandler.class);
		GroupMessageEvent group = (GroupMessageEvent) pjp.getArgs()[0]; // 假设 payload 是第一个参数
		String command = annotation.startWith();
		String params = group.getRawMessage().replace(command, "").trim();
		group.setMessage(params);
		proceed = group.getRawMessage().startsWith(command);
		String condition = annotation.condition();

		if (proceed) {
			log.info("AOP: Condition '{}' met. Proceeding with handler method: {}", condition, method.getName());
			return pjp.proceed();
		} else {
			log.info("AOP: Condition '{}' not met. Skipping handler method: {}", condition, method.getName());
			return null; // 阻止方法执行
		}
	}
}