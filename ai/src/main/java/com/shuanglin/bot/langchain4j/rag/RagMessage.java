package com.shuanglin.bot.langchain4j.rag;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;

public class RagMessage implements ChatMessage {
	@Override
	public ChatMessageType type() {
		return null;
	}
}
