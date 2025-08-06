package com.shuanglin.bot.langchain4j.tools;

import com.google.gson.JsonObject;
import com.shuanglin.bot.langchain4j.config.DocumentInitializer;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.service.MemoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentInsertTool {
	private final DocumentInitializer documentInitializer;

	@Tool("将用户问题作为知识进行入库")
	void insertDocument(@ToolMemoryId JsonObject params) {
		documentInitializer.learnStr(params,params.get("message").getAsString());
	}
}
