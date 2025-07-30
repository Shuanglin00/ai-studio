package com.shuanglin.bot.langchain4j.config.store;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shuanglin.enums.MongoDBConstant;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
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

	@Override
	public List<ChatMessage> getMessages(Object json) {
		JsonObject params = gson.toJsonTree(gson.fromJson(gson.toJsonTree(json), DbQueryVO.class)).getAsJsonObject();
		Query query = getQuery(params);
		Set<ChatMessage> chatMessages = new HashSet<>(mongoTemplate.find(query, ChatMessage.class));
		return List.of();
	}

	@Override
	public void updateMessages(Object json, List<ChatMessage> messages) {
		JsonObject params = gson.toJsonTree(gson.fromJson(gson.toJsonTree(json), DbQueryVO.class)).getAsJsonObject();
		String id = IdUtil.getSnowflakeNextIdStr();
		if (params.get("memoryId") != null) {
			params.addProperty("type", MongoDBConstant.StoreType.memory.name());
		} else {
			params.addProperty("type", MongoDBConstant.StoreType.nonMemory.name());
		}
		Query query = getQuery(params);
		List<String> list = messages.stream().map(item -> {
			if (item instanceof UserMessage message) {
				return message.singleText();
			}
			return "";
		}).toList();
		Update update = new Update();
		update.push("messages").each(list);
		update.setOnInsert("id", id);
		mongoTemplate.upsert(query, update, ChatMessage.class);
		params.addProperty("id", id);
		params.addProperty("embeddings", list.size());
		milvusClientV2.upsert(UpsertReq.builder().collectionName("").data(Collections.singletonList(params)).build());
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
