package com.shuanglin.bot.db;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("model_knowledge")
@Data
@Builder
public class KnowledgeEntity {

	private String id;

	private String type;

	private String content;

	private String modelId;

	private String modelName;

	private String userId;

	private String groupId;

	private Long lastChatTime;
}
