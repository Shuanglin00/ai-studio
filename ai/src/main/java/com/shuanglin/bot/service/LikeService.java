package com.shuanglin.bot.service;

import cn.hutool.core.util.IdUtil;
import com.google.gson.Gson;
import com.shuanglin.bot.utils.FileReadUtil;
import com.shuanglin.dao.message.MessageStoreEntity;
import com.shuanglin.dao.message.MessageStoreEntityRepository;
import com.shuanglin.dao.milvus.MessageEmbeddingEntity;
import com.shuanglin.dao.milvus.MessageEmbeddingMapper;
import com.shuanglin.enums.MongoDBConstant;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import jakarta.annotation.Resource;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.dromara.milvus.plus.model.vo.MilvusResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LikeService {
	@Resource
	private MessageStoreEntityRepository messageStoreEntityRepository;

	@Resource
	private MessageEmbeddingMapper messageEmbeddingMapper;

	@Resource
	private MilvusClientV2 milvusClientV2;

	@Resource
	private EmbeddingModel embeddingModel;

	@Resource(name = "decomposeLanguageModel")
	private OllamaChatModel decomposeLanguageModel;

	@Resource
	private Gson gson;

	public PromptTemplate likePromptTemplate() {
		return PromptTemplate.from("""
				你是一个风格化响应引擎。请将以下“用户输入”按“风格范文”的语言风格重写，输出一段风格一致的新文本。
				
				【风格范文】
				{{style_examples}}
				
				【用户输入】
				{{user_input}}
				
				【重写规则】
				1. 保持用户输入的核心意图（如是问题则回答，如是陈述则改写，如是主题则扩展）；
				2. 语言风格必须与“风格范文”高度一致（句式、语气、节奏、用词）；
				3. 输出必须流畅自然，符合目标风格。
				4. 确保语意通顺连贯。
				
				→ 请直接输出风格化结果，不要解释：
				"""
		);
	}

	public String chat(String question) {
		List<Float> floats = embeddingModel.embed(question).content().vectorAsList();
		List<MilvusResult<MessageEmbeddingEntity>> d	ata = messageEmbeddingMapper.queryWrapper().vector(floats).topK(5).query().getData();
		List<String> storeIds = data.stream().map(match -> match.getEntity().getStoreId()).toList();
		List<MessageStoreEntity> messageStoreEntities = messageStoreEntityRepository.findAllById(storeIds);
		String examples = messageStoreEntities.stream().map(MessageStoreEntity::getContent).collect(Collectors.joining("\n"));

		String prompt = likePromptTemplate().template().replace("{{style_examples}}", examples).replace("{{user_input}}", question);
		String chat = decomposeLanguageModel.chat(prompt);
		System.out.println("chat = " + chat);
		return chat;
	}
	public void loadKnowledge(){
		try {
			String string = FileReadUtil.readFileContent("D:\\project\\ai-studio\\file\\mi.txt");
			List<String> arrayList = new ArrayList<>(List.of(string.split("\n")));
			arrayList.forEach(item->{
				MessageStoreEntity messageStoreEntity = MessageStoreEntity.builder()
						.type(MongoDBConstant.StoreType.document.name())
						.content(item.trim())
						.id(IdUtil.getSnowflakeNextIdStr())
						.build();
				MessageEmbeddingEntity messageEmbeddingEntity = MessageEmbeddingEntity.builder()
						.embeddings(embeddingModel.embed(item.trim()).content().vector())
						.storeId(messageStoreEntity.getId())
						.storeType(messageStoreEntity.getType())
						.id("" + IdUtil.getSnowflakeNextIdStr().toString())
						.modelName("卡拉比喵")
						.build();
				InsertReq chatEmbeddingCollection = InsertReq.builder().data(Collections.singletonList(gson.toJsonTree(messageEmbeddingEntity).getAsJsonObject()))
						.collectionName("chatEmbeddingCollection")
						.build();
				milvusClientV2.insert(chatEmbeddingCollection);
				messageStoreEntityRepository.save(messageStoreEntity);
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InvalidFormatException e) {
			throw new RuntimeException(e);
		}
	}
}
