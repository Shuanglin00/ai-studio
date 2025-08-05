package com.shuanglin.bot.langchain4j.config.store;

import cn.hutool.core.util.IdUtil;
import com.google.gson.*;
import com.shuanglin.bot.db.MessageStoreEntity;
import com.shuanglin.bot.langchain4j.config.rag.embedding.vo.EmbeddingEntity;
import com.shuanglin.enums.MongoDBConstant;
import com.shuanglin.utils.JsonUtils;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
		return Collections.singletonList(UserMessage.from(flatten.get("message").getAsString()));
//		return ChatMessageDeserializer.messagesFromJson(chatMessages.stream().map(MessageStoreEntity::getContent).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString());
//		return List.of();
	}

	@Override
	public void updateMessages(Object json, List<ChatMessage> messages) {
		JsonObject params = JsonUtils.flatten(gson.toJsonTree(json).getAsJsonObject());
		JsonObject queryParams = gson.toJsonTree(gson.fromJson(params, DbQueryVO.class)).getAsJsonObject();
		EmbeddingEntity embeddingEntity = gson.fromJson(params, EmbeddingEntity.class);
		String id = IdUtil.getSnowflakeNextIdStr();
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
		if (messageContents.size() > 1) {
			throw new RuntimeException();
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
			MessageStoreEntity newEntity = gson.fromJson(params, MessageStoreEntity.class);
			Set<String> validPropertyNames = new HashSet<>();
			Class<?> currentClass = MessageStoreEntity.class;
			// 遍历当前类及其所有父类（直到 Object 类），以获取所有继承的字段
			while (currentClass != null && !currentClass.equals(Object.class)) {
				for (Field field : currentClass.getDeclaredFields()) {
					validPropertyNames.add(field.getName());
				}
				currentClass = currentClass.getSuperclass();
			}
			System.out.println("\nMessageStoreEntity 中所有合法的属性名: \n" + validPropertyNames);


			// 3. 创建目标对象，这里使用 Document，也可以是 new HashMap<String, Object>()
			Document targetDocument = new Document();

			// 预先设置一些固定值
			targetDocument.put("id", "some-generated-id");
			targetDocument.put("type", "nonMemory");
			targetDocument.put("content", "Some default content");

			// 4. 遍历 JsonObject，匹配属性并赋值
			Gson gson = new Gson(); // 用于将 JsonElement 转换回 Java 对象
			for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
				String key = entry.getKey();

				// 如果 JsonObject 的 key 存在于 POJO 的属性名集合中
				if (validPropertyNames.contains(key)) {
					JsonElement valueElement = entry.getValue();

					// 将 JsonElement 转换为合适的 Java 类型
					Object value = gson.fromJson(valueElement, Object.class);

					// 放入目标 Document 中
					targetDocument.put(key, value);
					System.out.println("匹配成功: key='" + key + "' 已被添加.");
				} else {
					System.out.println("匹配失败: key='" + key + "' 在 MessageStoreEntity 中不存在，已忽略.");
				}
			}
			mongoTemplate.insert(newEntity);
		}
		embeddingEntity.setEmbeddings(embeddingModel.embed(messageContents.get(0)).content().vector());
		embeddingEntity.setId(IdUtil.getSnowflakeNextIdStr());
		embeddingEntity.setMessageId(id);
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