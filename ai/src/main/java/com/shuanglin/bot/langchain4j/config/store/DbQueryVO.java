package com.shuanglin.bot.langchain4j.config.store;

import lombok.Data;

@Data
public class DbQueryVO {
	private String memoryId;
	private String type;
	private String userId;
	private String groupId;
	private String modelName;
}
