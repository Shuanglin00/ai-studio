package com.shuanglin.bot.langchain4j.config;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shuanglin.bot.db.KnowledgeEntity;
import com.shuanglin.bot.db.KnowledgeEntityRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

/**
 * 非会话记录存储，每一次都是模型知识
 *
 * @author lin
 * @date 2025/07/23
 */
@RequiredArgsConstructor
@Configuration
@Slf4j
public class NonMemoryStore implements ChatMemoryStore {
	private Gson gson;

	private KnowledgeEntityRepository knowledgeRepository;

	@Override
	public List<ChatMessage> getMessages(Object modelId) {
		return List.of();
	}

	@Override
	public void updateMessages(Object modelId, List<ChatMessage> messages) {
		JsonObject params = new JsonObject();
		String question = "";
		for (ChatMessage message : messages) {
			if (message instanceof SystemMessage) {
				params = gson.fromJson(((SystemMessage) message).text(), JsonObject.class);
			}
			if (message instanceof UserMessage) {
				log.info("((UserMessage) message).contents().toString() ======= {}", ((UserMessage) message).contents().toString());
				question = ((UserMessage) message).contents().toString();
			}
		}
		KnowledgeEntity knowledge = KnowledgeEntity.builder()
				.id(IdUtil.getSnowflakeNextIdStr())
				.type("message")
				.groupId(params.get("groupId").getAsString())
				.userId(params.get("userId").getAsString())
				.content(question)
				.lastChatTime(System.currentTimeMillis())
				.modelName(modelId.toString())
				.build();
		knowledgeRepository.save(knowledge);
	}

	@Override
	public void deleteMessages(Object modelId) {

	}
}
