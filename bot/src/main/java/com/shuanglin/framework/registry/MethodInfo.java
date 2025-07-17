package com.shuanglin.framework.registry;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 存储被注解的处理器方法的所有元数据。
 *
 * @param bean       方法所在的 Spring Bean 实例（代理对象）
 * @param method     方法本身
 * @param annotation 方法上的注解实例
 */
public record MethodInfo(Object bean, Method method, Annotation annotation) {
}