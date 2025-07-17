package com.shuanglin.framework.annotation;

import com.shuanglin.framework.enums.ListenerType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为 "group" 类型的消息处理器。
 * 框架可以扩展出更多类似的注解，如 @PrivateMessageHandler, @SystemMessageHandler 等。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupMessageHandler {
	/**
	 * SpEL表达式，用于在方法执行前进行条件判断。
	 * 表达式的根对象是消息的 payload。
	 * 例如: "#payload['level'] > 5"
	 * 默认为 "true"，表示无条件执行。
	 */
	String condition() default "true";

	/**
	 * 定义监听器的行为。
	 */
	ListenerType listenerType() default ListenerType.ALWAYS;
}