package com.shuanglin.bot.langchain4j.rag.contentInjector;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class multiStepContentInjector implements ContentInjector {

	public PromptTemplate multiStepPromptTemplate() {
		return PromptTemplate.from("""
				前提剧情概要：
				{{content}}
				用户提问:
				{{userMessage}}
				""");
	}

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
		Prompt apply = multiStepPromptTemplate().apply(params);
		return apply.toUserMessage();
	}
}
