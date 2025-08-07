package com.shuanglin.bot.langchain4j.rag.contentInjector;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Component("chatContentInjector")
@Slf4j
public class ChatContentInjector implements ContentInjector {
	@Resource(name = "storePromptTemplate")
	private PromptTemplate storePromptTemplate;

	@Override
	public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
		if (contents.isEmpty()) {
			return chatMessage;
		}
		Map<String, Object> params = contents.get(0).textSegment().metadata().toMap();
		StringJoiner collect = contents.stream().map(Content::textSegment).map(TextSegment::text)
				.collect(() -> new StringJoiner("\n"), StringJoiner::add, StringJoiner::merge);
		params.put("content", collect);
		params.put("userMessage", ((UserMessage) chatMessage).singleText());
//		params.put("modelName",params.getOrDefault("modelName",""));
//		params.put("instruction",params.getOrDefault("instruction",""));
//		params.put("description",params.getOrDefault("description",""));
		Prompt apply = storePromptTemplate.apply(params);
		return apply.toSystemMessage();
	}
}
