package com.shuanglin.bot.langchain4j.config;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.db.KnowledgeEntity;
import com.shuanglin.bot.db.KnowledgeEntityRepository;
import com.shuanglin.bot.langchain4j.rag.embedding.vo.EmbeddingEntity;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 非会话记录存储，每一次都是模型知识
 *
 * @author lin
 * @date 2025/07/23
 */
@RequiredArgsConstructor
@Configuration
@Slf4j
@Component("NonMemoryStore")
public class NonMemoryStore implements ChatMemoryStore {
	private final Gson gson;

	private final KnowledgeEntityRepository knowledgeRepository;

	private final EmbeddingModel embeddingModel;

	private final MilvusClientV2 milvusClient;


	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名


	@Override
	public List<ChatMessage> getMessages(Object object) {
		JsonObject params = gson.toJsonTree(object).getAsJsonObject();
		JsonObject senderInfo = gson.toJsonTree(params.get("senderInfo")).getAsJsonObject();
		JsonObject group = gson.toJsonTree(params.get("groupMessageEvent")).getAsJsonObject();
		String groupId = group.get("group_id").getAsString();
		String userId = group.get("user_id").getAsString();
		String modelId = senderInfo.get("modelInfo").getAsJsonObject().get("useModel").getAsString();
		knowledgeRepository.findByGroupIdAndUserIdAndModelId(groupId, userId, modelId);
		List<Content> finalContentList = knowledgeRepository.findByGroupIdAndUserIdAndModelId(groupId, userId, modelId).stream()
				.map(document -> {
					// 在这里，你可以从 document 构建非常丰富的元数据
					String content = document.getContent();
					return Content.from(content);
				})
				.collect(Collectors.toList());
		return Collections.singletonList(new UserMessage(userId, String.valueOf(finalContentList)));
	}

	@Override
	public void updateMessages(Object object, List<ChatMessage> messages) {
		JsonObject params = gson.toJsonTree(object, JsonObject.class).getAsJsonObject();
		JsonObject senderInfo = gson.toJsonTree(params.get("senderInfo")).getAsJsonObject();
		JsonObject group = gson.toJsonTree(params.get("groupMessageEvent")).getAsJsonObject();
			KnowledgeEntity knowledge = KnowledgeEntity.builder()
					.id(IdUtil.getSnowflakeNextIdStr())
					.type("message")
					.groupId(group.get("group_id").getAsString())
					.userId(group.get("user_id").getAsString())
					.content(group.get("message").getAsString())
					.lastChatTime(System.currentTimeMillis())
					.modelId(senderInfo.get("modelInfo").getAsJsonObject().get("useModel").getAsString())
					.build();
			knowledgeRepository.save(knowledge);
				JsonObject jsonObject = new Gson().toJsonTree(EmbeddingEntity.builder()
				.userId(group.get("user_id").getAsString())
				.groupId(group.get("group_id").getAsString())
				.embeddings(embeddingModel.embed(group.get("message").getAsString()).content().vector())
				.memoryId(IdUtil.getSnowflakeNextIdStr())
				.build()).getAsJsonObject();
		milvusClient.upsert(UpsertReq.builder().collectionName(defaultCollectionName).data(Collections.singletonList(jsonObject)).build());
	}

	@Override
	public void deleteMessages(Object sender) {

	}
}
