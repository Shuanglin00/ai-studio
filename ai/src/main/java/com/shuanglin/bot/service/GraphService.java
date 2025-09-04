package com.shuanglin.bot.service;

import com.shuanglin.bot.langchain4j.assistant.DecomposeAssistant;
import com.shuanglin.bot.utils.FileReadUtil;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphService {
	@Resource(name = "decomposeLanguageModel")
	private OllamaChatModel decomposeLanguageModel;

	DecomposeAssistant assistant = AiServices.builder(DecomposeAssistant.class).chatModel(decomposeLanguageModel).build();

	public void readStory() {
		File storyFile = new File("./src/main/resources/story.txt");
		List<FileReadUtil.ParseResult> parseResults = FileReadUtil.readEpubFile(storyFile);
		for (FileReadUtil.ParseResult parseResult : parseResults) {
			for (int i = 1; i < parseResult.getContentList().size() - 2; i++) {
				String s = assistant.enhancedEntityExtraction(parseResult.getContentList().get(i), parseResult.getContentList().get(i - 1), parseResult.getContentList().get(i + 1));
				System.out.println("s = " + s);
			}
			break;
		}
	}
}
