package com.shuanglin.aop.annotation;

import java.lang.annotation.*;

import static com.shuanglin.aop.enums.EventConstant.CMD_DEFAULT_STAFF_VALUE;

@Documented
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandFilter {
	/**
	 * 触发命令，支持正则
	 * 注: 仅用于消息校验, 不会返回 matcher (理论上可以做到, 但是会冲突
	 *
	 * @return 正则表达式
	 */
	String cmd() default CMD_DEFAULT_STAFF_VALUE;

	/**
	 * 若指明前缀, 则仅消息头部匹配前缀的消息才可以触发, 判断条件为or, 如果为空则任意消息都可以触发
	 *
	 * @return 前缀, 可多选
	 */
	String[] startWith() default {};

	String[] contains() default "";
}
