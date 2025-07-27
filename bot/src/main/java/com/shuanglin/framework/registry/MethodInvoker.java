package com.shuanglin.framework.registry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MethodInvoker {

	public void invoke(MethodInfo info, Object payload) {
		try {
			// 调用 info.bean() 上的方法，确保 AOP 切面能拦截
			info.method().invoke(info.bean(), payload);
		} catch (Exception e) {
			log.error("Failed to invoke message handler method: {}", info.method().getName(), e);
			log.info(e.getMessage());
		}
	}
}