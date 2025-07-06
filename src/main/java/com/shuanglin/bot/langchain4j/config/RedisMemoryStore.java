package com.shuanglin.bot.langchain4j.config;

import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.result.UpdateResult;
import com.shuanglin.bot.config.DBMessageDTO;
import com.shuanglin.bot.langchain4j.rag.embedding.vo.EmbeddingEntity;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class RedisMemoryStore implements ChatMemoryStore {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private MilvusClientV2 milvusClientV2;

	@Autowired
	private EmbeddingModel embeddingModel;

	@Value("${spring.data.milvus.defaultDatabaseName}")
	private String defaultDatabaseName; // 默认数据库名

	@Value("${spring.data.milvus.defaultCollectionName}")
	private String defaultCollectionName; // 默认集合名

	@Override
	public List<ChatMessage> getMessages(Object memoryId) {
		System.out.println("[Memory] 加载历史, memoryId=" + memoryId);
		String redisMessage = redisTemplate.opsForValue().get(memoryId.toString());
		if (redisMessage == null) {
			Query query = new Query(Criteria.where("memoryId").is(memoryId));
			DBMessageDTO dbMessage = mongoTemplate.findOne(query, DBMessageDTO.class);
			if (dbMessage != null) {
				redisTemplate.opsForValue().set(memoryId.toString(), dbMessage.getContent());
				return ChatMessageDeserializer.messagesFromJson(dbMessage.getContent());
			} else {
				this.updateMessages(memoryId,new ArrayList<>());
			}
		} else {
			return ChatMessageDeserializer.messagesFromJson(redisMessage);
		}
		return List.of();
	}

	@Override
	public void updateMessages(Object memoryId, List<ChatMessage> list) {
		String userId = "";
		List<UserMessage> userMessages = new ArrayList<>();
		if (!list.isEmpty()) {
			for (ChatMessage chatMessage : list) {
				if (chatMessage instanceof UserMessage userMessage) {
					if (StrUtil.isNotBlank(userMessage.name())) {
						userId = userMessage.name();
						userMessages.add(new UserMessage(userMessage.contents().get(0)));
						break;
					}
				}
			}
		}
		System.out.println("list = " + new Gson().toJson(list));
		// 分爲userMessage 和AiMessage 需要抽離分開存儲 獲取的時候只獲取userMessage 拼接 至於AiMessage後續再看
		Criteria criteria = Criteria.where("memoryId").is(memoryId);
		Query query = new Query(criteria);
		Update update = new Update();
		update.set("content", ChatMessageSerializer.messagesToJson(list));
		update.set("userId", userId);
		update.set("memoryId", memoryId);
		update.set("lastChatTime", System.currentTimeMillis());
		UpdateResult upsert = mongoTemplate.upsert(query, update, DBMessageDTO.class);
		JsonObject jsonObject = new Gson().toJsonTree(EmbeddingEntity.builder()
				.userId(userId)
				.groupId(userId)
				.embeddings(embeddingModel.embed(userMessages.toString()).content().vector())
				.memoryId(memoryId.toString())
				.build()).getAsJsonObject();
		milvusClientV2.upsert(UpsertReq.builder().collectionName(defaultCollectionName).data(Collections.singletonList(jsonObject)).build());
		if (upsert.getModifiedCount() == 0 && upsert.getUpsertedId() == null) {
			throw new RuntimeException("no find message by id: " + memoryId);
		} else {
			redisTemplate.delete(memoryId.toString());
		}
	}

	@Override
	public void deleteMessages(Object memoryId) {
		System.out.println("[Memory] 删除历史, memoryId=" + memoryId);
		Criteria criteria = Criteria.where("memoryId").is(memoryId);
		Query query = new Query(criteria);
		mongoTemplate.remove(query, DBMessageDTO.class);
		redisTemplate.delete(memoryId.toString());
	}
}