package com.shuanglin.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在Controller方法上，声明该方法的返回值将作为消息发布到总线。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishBus {
	/**
	 * 定义消息的类型，用于下游消费者过滤。
	 */
	String type();
}