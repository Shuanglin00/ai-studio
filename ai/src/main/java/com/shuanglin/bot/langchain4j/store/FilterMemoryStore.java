package com.shuanglin.bot.langchain4j.store;

import cn.hutool.core.util.IdUtil;
import com.google.gson.*;
import com.shuanglin.bot.db.MessageStoreEntity;
import com.shuanglin.bot.langchain4j.rag.embedding.vo.EmbeddingEntity;
import com.shuanglin.utils.JsonUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("filterMemoryStore")
@RequiredArgsConstructor
public class FilterMemoryStore implements ChatMemoryStore {
	private final Gson gson;
	private final MongoTemplate mongoTemplate;
	private final MilvusClientV2 milvusClientV2;
	private final EmbeddingModel embeddingModel;

	@Override
	public List<ChatMessage> getMessages(Object json) {
		JsonObject flatten = JsonUtils.flatten(gson.toJsonTree(json).getAsJsonObject());
		if (flatten.has("messageId")) {
			org.springframework.data.mongodb.core.query.Query mongoQuery = new org.springframework.data.mongodb.core.query.Query();
			mongoQuery.addCriteria(Criteria.where("messageId").in(flatten.get("messageId").getAsString()));
			List<MessageStoreEntity> mongoResult = mongoTemplate.find(mongoQuery, MessageStoreEntity.class);
			if (mongoResult.isEmpty()) {
				return Collections.emptyList();
			}
			return Collections.singletonList(UserMessage.from(mongoResult.get(0).getContent()));
		}
		return List.of();
	}

	@Override
	public void updateMessages(Object json, List<ChatMessage> messages) {
		JsonObject params = JsonUtils.flatten(gson.toJsonTree(json).getAsJsonObject());
		JsonObject queryParams = gson.toJsonTree(gson.fromJson(params, DbQueryVO.class)).getAsJsonObject();
		EmbeddingEntity embeddingEntity = gson.fromJson(params, EmbeddingEntity.class);
		String messageId = params.get("messageId").getAsString();
		List<String> messageContents = messages.stream()
				.map(item -> {
					if (item instanceof UserMessage message) {
						return message.singleText();
					}
					return null; // 使用 null 代替空字符串，方便过滤
				})
				.filter(Objects::nonNull)
				.toList();

// 如果没有有效消息内容，则不执行任何数据库操作
		if (messageContents.isEmpty()) {
			return; // 或者 continue/break，取决于您的外部循环结构
		}

// 2. 检查 'memoryId' 是否存在且有效
		if (queryParams.has("memoryId") && !queryParams.get("memoryId").isJsonNull() && !queryParams.get("memoryId").getAsString().isEmpty()) {
			embeddingEntity.setMemoryId(queryParams.get("memoryId").getAsString());
			// --- 场景一: memoryId 存在 - 执行追加更新 ---
			String memoryId = queryParams.get("memoryId").getAsString();

			// a. 构建查询条件，通过 memoryId 找到要更新的文档
			Query query = Query.query(Criteria.where("memoryId").is(memoryId));

			// b. 构建更新操作，只包含 $push 指令
			Update update = new Update();
			update.push("content").each(messageContents);

			// c. 执行更新，将新消息追加到找到的文档中
			mongoTemplate.updateFirst(query, update, MessageStoreEntity.class);

		} else {

			// --- 场景二: memoryId 不存在 - 创建新文档 ---
			embeddingEntity.setMemoryId("");
			// a. 创建并完整填充一个新的实体对象
			Query query = Query.query(Criteria.where("messageId").is(messageId));

			// b. 构建更新操作，只包含 $push 指令
			Update update = new Update();
			update.addToSet("userId", params.get("user_id").getAsString());
			update.addToSet("groupId", params.get("group_id").getAsString());
			update.push("content").each(messageContents);

			// c. 执行更新，将新消息追加到找到的文档中
			mongoTemplate.upsert(query, update, MessageStoreEntity.class);
		}
		embeddingEntity.setEmbeddings(embeddingModel.embed(messageContents.get(0)).content().vector());
		embeddingEntity.setId(IdUtil.getSnowflakeNextIdStr());
		embeddingEntity.setMessageId(messageId);
		milvusClientV2.upsert(UpsertReq.builder().collectionName("rag_embedding_collection").data(Collections.singletonList(gson.toJsonTree(embeddingEntity).getAsJsonObject())).build());
	}

	@NotNull
	private Query getQuery(JsonObject params) {
		Query query = new Query();
		for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
			query.addCriteria(Criteria.where(entry.getKey()).is(entry.getValue().getAsString()));
		}
		return query;
	}

	@Override
	public void deleteMessages(Object json) {

	}
}