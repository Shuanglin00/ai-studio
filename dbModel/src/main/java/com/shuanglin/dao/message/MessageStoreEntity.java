package com.shuanglin.dao.message;

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

	private String id;

	private String messageId;

	private String memoryId;

	private String type;

	private String content;

	private Long lastChatTime;
}
