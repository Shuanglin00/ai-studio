package com.shuanglin.bot.langchain4j.assistant;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DecomposeAssistant {

	@UserMessage("将以下问题分解为3个或更少的、更简单的子问题。只返回一个以；分号分隔的子问题列表，不要添加任何其他文本。问题：{{query}}")
	String decompose(@V("query") String query);
}
