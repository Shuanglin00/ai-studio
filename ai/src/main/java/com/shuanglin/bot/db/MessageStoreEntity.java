package com.shuanglin.bot.db;

import com.google.gson.annotations.SerializedName;
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

	@SerializedName("messageId")
	private String id;

	private String type;

	private String memoryId;

	private String content;

	private String modelName;

	@SerializedName(value = "userId", alternate = {"user_id"})
	private String userId;

	@SerializedName(value = "groupId", alternate = {"group_id"})
	private String groupId;

	private Long lastChatTime;
}
