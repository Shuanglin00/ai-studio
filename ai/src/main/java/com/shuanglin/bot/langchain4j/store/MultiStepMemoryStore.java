package com.shuanglin.bot.langchain4j.store;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.langchain4j.config.vo.MilvusProperties;
import com.shuanglin.dao.milvus.MessageEmbeddingEntity;
import com.shuanglin.dao.message.MessageStoreEntity;
import com.shuanglin.enums.MongoDBConstant;
import com.shuanglin.utils.JsonUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 根据messageId，systemMessage存储RAG检索知识库
 * 根据messageId检索history
 *
 * @author lin
 * @date 2025/08/07
 */
@Component("multiStepMemoryStore")
@RequiredArgsConstructor
public class MultiStepMemoryStore implements ChatMemoryStore {

	private final Gson gson;

	private final MongoTemplate mongoTemplate;

	private final MilvusClientV2 milvusClientV2;

	private final EmbeddingModel embeddingModel;

	private final MilvusProperties milvusProperties;

	/**
	 * 获取消息
	 *
	 * @param memoryId 内存id
	 * @return {@code List<ChatMessage> }
	 */
	@Override
	public List<ChatMessage> getMessages(Object memoryId) {
		JsonObject flatten = JsonUtils.flatten(gson.toJsonTree(memoryId).getAsJsonObject());
		if (!flatten.has("memoryId")) {
			org.springframework.data.mongodb.core.query.Query mongoQuery = new org.springframework.data.mongodb.core.query.Query();
			mongoQuery.addCriteria(Criteria.where("messageId").is(flatten.get("messageId").getAsString()));
			MessageStoreEntity mongoResult = mongoTemplate.findOne(mongoQuery, MessageStoreEntity.class);
			if (mongoResult == null) {
				return Collections.emptyList();
			}
			return ChatMessageDeserializer.messagesFromJson(mongoResult.getContent());
		} else {
			org.springframework.data.mongodb.core.query.Query mongoQuery = new org.springframework.data.mongodb.core.query.Query();
			mongoQuery.addCriteria(Criteria.where("memoryId").in(flatten.get("memoryId").getAsString()));
			List<MessageStoreEntity> mongoResult = mongoTemplate.find(mongoQuery, MessageStoreEntity.class);
			if (mongoResult.isEmpty()) {
				return Collections.emptyList();
			}
			return mongoResult.stream().map(messages -> ChatMessageDeserializer.messagesFromJson(messages.getContent())).flatMap(Collection::stream).toList();
		}
	}

	@Override
	public void updateMessages(Object memoryId, List<ChatMessage> messages) {
		JsonObject params = JsonUtils.flatten(gson.toJsonTree(memoryId).getAsJsonObject());
		MessageEmbeddingEntity embeddingEntity = gson.fromJson(params, MessageEmbeddingEntity.class);
		embeddingEntity.setStoreType(MongoDBConstant.StoreType.memory.name());
		Query query = new Query();
		// b. 构建更新操作，只包含 $push 指令
		Update update = new Update();
		// 如果没有有效消息内容，则不执行任何数据库操作
		if (messages.isEmpty()) {
			return;
		}
		update.set("id",IdUtil.getSnowflakeNextIdStr());
		update.set("messageId", params.get("messageId").getAsString());
		update.set("content", messages);
		mongoTemplate.upsert(query, update, MessageStoreEntity.class);
		// 只将userMessage存储向量库，即只存储消息会话向量
		List<TextSegment> list = messages.stream().map(message -> {
			if (message instanceof UserMessage) {
				return TextSegment.from(((UserMessage) message).singleText());
			}
			return null;
		}).toList();
		if (list.isEmpty()) {
			return;
		}
		List<Embedding> embeddings = embeddingModel.embedAll(list).content();
		for (Embedding embedding : embeddings) {
			embeddingEntity.setEmbeddings(embedding.vector());
			embeddingEntity.setId(IdUtil.getSnowflakeNextIdStr());
			milvusClientV2.upsert(UpsertReq.builder().collectionName(milvusProperties.getMessageCollectionName()).data(Collections.singletonList(gson.toJsonTree(embeddingEntity).getAsJsonObject())).build());
		}
	}

	@Override
	public void deleteMessages(Object memoryId) {

	}
}
