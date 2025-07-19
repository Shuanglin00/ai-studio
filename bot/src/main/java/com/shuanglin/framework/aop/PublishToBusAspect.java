package com.shuanglin.framework.aop;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.framework.annotation.PublishBus;
import com.shuanglin.framework.bus.MessageBus;
import com.shuanglin.framework.bus.event.Event;
import com.shuanglin.framework.bus.event.GroupMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PublishToBusAspect {

	private final MessageBus messageBus;

	// 定义切点，拦截所有被 @PublishToBus 注解的方法
	@Pointcut("@annotation(publishBus)")
	public void publishBusPointcut(PublishBus publishBus) {
	}

	// 在方法成功返回后执行
	@Around(value = "publishBusPointcut(publishBus)", argNames = "joinPoint,publishBus")
	public Object afterReturning(ProceedingJoinPoint joinPoint, PublishBus publishBus) throws Throwable {
		if (joinPoint.getArgs() != null) {
			String messageType = publishBus.type(); // 获取注解中定义的消息类型
			try {
				String arg = (String)joinPoint.getArgs()[0];
				messageBus.publish(new Gson().fromJson(arg, JsonObject.class));
			} catch (Exception e) {
				log.error("event {}",joinPoint.getArgs()[0]);
				log.error("AOP: Error while processing return value for message type '{}': {}", messageType, e.getMessage());
			}
		} else {
			log.warn("AOP: Method returned null, no message published.");
		}
		return joinPoint.proceed();
	}
}