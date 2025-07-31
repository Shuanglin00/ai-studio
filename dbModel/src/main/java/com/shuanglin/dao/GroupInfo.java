package com.shuanglin.dao;

import com.shuanglin.dao.model.ModelInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;

@Data
@AllArgsConstructor()
@Builder
public class GroupInfo {
	private static volatile GroupInfo instance;
	private GroupInfo() {
		// 避免耗时初始化
	}

	// 3. 公共静态同步方法，保证线程安全
	public static synchronized GroupInfo getInstance() {
		if (instance == null) { // 第一次检查
			synchronized (GroupInfo.class) {
				if (instance == null) { // 第二次检查
					instance = GroupInfo.builder()
							.modelInfo(ModelInfo.builder()
									.modelName("1")
									.activeModels(Collections.singletonList("1"))
									.build())
							.build();
				}
			}
		}
		return instance;
	}
	private String groupId;

	ModelInfo modelInfo;
}
