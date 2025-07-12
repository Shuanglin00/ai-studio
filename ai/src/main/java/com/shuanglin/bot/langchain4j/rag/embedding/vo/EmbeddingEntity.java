package com.shuanglin.bot.langchain4j.rag.embedding.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmbeddingEntity {
	private String id;
	private String userId;
	private String groupId;
	private float[] embeddings;
	private String memoryId;

}
