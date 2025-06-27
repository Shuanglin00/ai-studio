package com.shuanglin.bot.langchain4j.config;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import com.shuanglin.bot.config.DBMessageDTO;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RedisMemoryStore implements ChatMemoryStore {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public List<ChatMessage> getMessages(Object memoryId) {
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
		System.out.println("list = " + list);
		Criteria criteria = Criteria.where("memoryId").is(memoryId);
		Query query = new Query(criteria);
		Update update = new Update();
		update.set("content", ChatMessageSerializer.messagesToJson(list));
		UpdateResult upsert = mongoTemplate.upsert(query, update, DBMessageDTO.class);
		if (upsert.getModifiedCount() == 0 && upsert.getUpsertedId() == null) {
			throw new RuntimeException("no find message by id: " + memoryId);
		} else {
			redisTemplate.delete(memoryId.toString());
		}
	}

	@Override
	public void deleteMessages(Object memoryId) {
		Criteria criteria = Criteria.where("memoryId").is(memoryId);
		Query query = new Query(criteria);
		mongoTemplate.remove(query, DBMessageDTO.class);
		redisTemplate.delete(memoryId.toString());
	}
}