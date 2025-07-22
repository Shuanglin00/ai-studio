package com.shuanglin.dbModel.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;

/**
 * 维护发送人的信息
 *
 * @author lin
 * @date 2025/07/22
 */
@Data
@AllArgsConstructor
@Builder
public class SenderInfo {
	private static volatile SenderInfo instance;
	private SenderInfo() {
		// 避免耗时初始化
	}

	// 3. 公共静态同步方法，保证线程安全
	public static synchronized SenderInfo getInstance() {
		if (instance == null) { // 第一次检查
			synchronized (SenderInfo.class) {
				if (instance == null) { // 第二次检查
					instance = SenderInfo.builder()
							.userId("2784152733")
							.modelInfo(ModelInfo.builder()
									.useModel("1")
									.activeModels(Collections.singletonList("1"))
									.build())
							.build();
				}
			}
		}
		return instance;
	}


	private String userId;

	private String groupId;

	private ModelInfo modelInfo;
}
