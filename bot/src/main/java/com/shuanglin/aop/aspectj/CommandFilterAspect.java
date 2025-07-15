package com.shuanglin.aop.aspectj;

import com.shuanglin.aop.annotation.CommandFilter;
import com.shuanglin.aop.event.MessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.shuanglin.aop.enums.EventConstant.CMD_DEFAULT_STAFF_VALUE;

/**
 * 命令筛选器 切面
 * 用來處理{@link MessageEvent} 類型的指令消息
 * 处理系统定义的命令
 *
 * @author lin
 * @date 2025/07/15
 */
@Aspect
@Component
@Slf4j
public class CommandFilterAspect {

	/**
	 * 定义切点，匹配所有被 @CommandFilter 注解标记的方法。
	 */
	@Pointcut("@annotation(com.shuanglin.aop.annotation.CommandFilter)") // 替换为你的注解包路径
	public void commandFilteredMethod() {
	}

	/**
	 * 环绕通知，负责解析注解并直接执行过滤。
	 */
	@Around("commandFilteredMethod()")
	public Object doCommandFilter(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		CommandFilter annotation = method.getAnnotation(CommandFilter.class);

		// 1. 从切点获取消息上下文
		//    你需要根据你的实际代码来获取 MessageContext 对象。
		//    这里假设它作为第一个方法参数传递。
		Object[] args = joinPoint.getArgs();
		MessageEvent event = null;
		for (Object arg : args) {
			if (arg instanceof MessageEvent) {
				event = (MessageEvent) arg;
				break; //
			}
		}

		if (event == null) {
			// 如果没有找到 MessageContext，则跳过过滤
			log.warn("警告: 在方法 {} 中未找到 MessageContext 参数，将跳过过滤。", method.getName());
			return joinPoint.proceed();
		}

		// 2. 应用所有过滤条件
		assert annotation != null;
		boolean allConditionsMet = applyAllFilters(annotation, event);

		if (allConditionsMet) {
			log.info("CommandFilter: 所有条件满足，允许执行方法 {}", method.getName());
			return joinPoint.proceed(); // 继续执行目标方法
		} else {
			log.info("CommandFilter: 条件不完全满足，阻止执行方法 {}", method.getName());
			throw new RuntimeException("Command execution blocked by filter.");
		}
	}

	/**
	 * 应用注解中的所有过滤条件，返回是否所有条件都满足。
	 * 注意：这里将所有条件视为 AND 关系。
	 */
	private boolean applyAllFilters(CommandFilter annotation, MessageEvent messageEvent) {
		String cmd = annotation.cmd() == null ? CMD_DEFAULT_STAFF_VALUE : annotation.cmd();
		List<Boolean> conditionResults = new ArrayList<>();
		// 1. cmd() 属性
		conditionResults.add(messageEvent.getMessage().startsWith(cmd));

		// 2. startWith() 属性
		if (annotation.startWith().length > 0) {
			boolean startWithMatch = false;
			String content = messageEvent.getMessage();
			for (String prefix : annotation.startWith()) {
				if (content.startsWith(prefix)) {
					startWithMatch = true;
					break;
				}
			}
			conditionResults.add(startWithMatch);
		}
		// 3. contains() 属性
		if (annotation.startWith().length > 0) {
			boolean containsMatch = false;
			String content = messageEvent.getMessage();
			for (String contains : annotation.startWith()) {
				if (content.contains(contains)) {
					containsMatch = true;
					break;
				}
			}
			conditionResults.add(containsMatch);
		}
		return conditionResults.stream().allMatch(Predicate.isEqual(true));
	}
}