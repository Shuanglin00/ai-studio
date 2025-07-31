package com.shuanglin.bot.langchain4j.config.store;

import cn.hutool.core.util.IdUtil;
import com.google.gson.*;
import com.shuanglin.bot.db.MessageStoreEntity;
import com.shuanglin.bot.langchain4j.config.rag.embedding.vo.EmbeddingEntity;
import com.shuanglin.enums.MongoDBConstant;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
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
		JsonObject params = gson.toJsonTree(gson.fromJson(flatten(gson.toJsonTree(json).getAsJsonObject()), DbQueryVO.class)).getAsJsonObject();
		Query query = getQuery(params);
		Set<MessageStoreEntity> chatMessages = new HashSet<>(mongoTemplate.find(query, MessageStoreEntity.class));
//		return ChatMessageDeserializer.messagesFromJson(chatMessages.stream().map(MessageStoreEntity::getContent).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString());
		return List.of();
	}

	@Override
	public void updateMessages(Object json, List<ChatMessage> messages) {
		JsonObject params = gson.toJsonTree(gson.fromJson(flatten(gson.toJsonTree(json).getAsJsonObject()), DbQueryVO.class)).getAsJsonObject();
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
		if (params.has("memoryId") && !params.get("memoryId").isJsonNull() && !params.get("memoryId").getAsString().isEmpty()) {
			embeddingEntity.setMemoryId(params.get("memoryId").getAsString());
			// --- 场景一: memoryId 存在 - 执行追加更新 ---
			String memoryId = params.get("memoryId").getAsString();

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
			MessageStoreEntity newEntity = new MessageStoreEntity();
			newEntity.setId(id);
			newEntity.setType(MongoDBConstant.StoreType.nonMemory.name());
			newEntity.setContent(messageContents.toString());

			mongoTemplate.insert(newEntity);
		}
		params.addProperty("messageId", id);
		embeddingEntity.setEmbeddings(embeddingModel.embed(messageContents.get(0)).content().vector());
		embeddingEntity.setId(IdUtil.getSnowflakeNextIdStr());
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

	public static JsonObject flatten(JsonObject sourceObject) {
		// 最终返回的扁平化对象
		JsonObject flattenedObject = new JsonObject();

		// 创建一个栈，用于存放待处理的 JsonObject
		Stack<JsonObject> processingStack = new Stack<>();

		// 将最外层的源对象首先压入栈中
		processingStack.push(sourceObject);

		// 当栈中还有待处理的对象时，循环继续
		while (!processingStack.isEmpty()) {
			// 从栈中弹出一个对象进行处理
			JsonObject currentObject = processingStack.pop();

			// 遍历当前对象的所有属性
			for (Map.Entry<String, JsonElement> entry : currentObject.entrySet()) {
				String key = entry.getKey();
				JsonElement value = entry.getValue();

				// 检查属性的值是否是另一个 JsonObject
				if (value.isJsonObject()) {
					// 如果是，则将这个嵌套的 JsonObject 压入栈中，以便后续处理
					processingStack.push(value.getAsJsonObject());
				} else {
					// 如果值不是 JsonObject（即是原始值、数组或 null），
					// 这就是我们想要的“叶子节点”。
					// 直接将其原始的键和值添加到最终的扁平化对象中。
					// 注意：如果键已存在，此操作会覆盖旧值。
					flattenedObject.add(key, value);
				}
			}
		}

		return flattenedObject;
	}
}