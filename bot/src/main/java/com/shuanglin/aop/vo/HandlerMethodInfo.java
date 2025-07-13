package com.shuanglin.aop.vo;
import java.lang.reflect.Method;

/**
 * 封装一个处理器方法及其所属的Bean实例，用于后续反射调用。
 */
public class HandlerMethodInfo {
	private final Object beanInstance;
	private final Method method;

	public HandlerMethodInfo(Object beanInstance, Method method) {
		this.beanInstance = beanInstance;
		this.method = method;
	}

	public Object getBeanInstance() {
		return beanInstance;
	}

	public Method getMethod() {
		return method;
	}

	@Override
	public String toString() {
		return "Handler{" +
				"class=" + beanInstance.getClass().getSimpleName() +
				", method=" + method.getName() +
				'}';
	}
}