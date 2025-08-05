package com.shuanglin.bot.db;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("message_store")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageStoreEntity {

	private String id;

	private String type;

	private String memoryId;

	private String content;

	private String modelName;

	private String userId;

	private String groupId;

	private Long lastChatTime;
}
