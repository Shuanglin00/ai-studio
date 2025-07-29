package com.shuanglin.bot.langchain4j.rag.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("chatContentInjector")
@RequiredArgsConstructor
public class ChatContentInjector implements ContentInjector {

	private final PromptTemplate defaultPromptTemplate;

	@Override
	public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
		return defaultPromptTemplate.apply(contents).toUserMessage();
	}
}
