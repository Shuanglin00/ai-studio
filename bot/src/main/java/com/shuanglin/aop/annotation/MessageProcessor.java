package com.shuanglin.aop.annotation;

import com.shuanglin.aop.enums.EventConstant.MessageEventType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  标记一个方法，表示该方法是一个消息处理器。
 *  通过 'type' 参数可以指定具体处理的消息类型。
 *  这个注解本身不强制执行任何逻辑，逻辑是通过 AOP 或其他框架配合实现的。
 */
@Retention(RetentionPolicy.RUNTIME) // 或者 RetentionPolicy.CLASS 如果不需要运行时反射
@Target(ElementType.METHOD)       // 只能用于方法
public @interface MessageProcessor {
	/**
	 *  指定此方法处理的消息类型。
	 *  例如：GROUP, PRIVATE, FRIEND 等。
	 *  @return 消息类型
	 */
	MessageEventType type();

	/**
	 *  (可选) 可以用来描述这个处理器具体做什么，比如 "store", "validate", "forward"。
	 *  如果所有带这个注解的方法都执行相同的存储逻辑，这个字段可以省略，
	 *  或者直接把注解命名为 @StoreMessage。
	 *  这里提供一个更通用的命名，允许为不同的处理器添加不同的操作描述。
	 */
	// String action() default "process";
}
