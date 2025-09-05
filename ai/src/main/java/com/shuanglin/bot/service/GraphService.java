package com.shuanglin.bot.service;

import com.shuanglin.bot.langchain4j.assistant.DecomposeAssistant;
import com.shuanglin.bot.utils.FileReadUtil;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class GraphService {
	private static final String NEO4J_URI = "bolt://8.138.204.38:7687";
	private static final String NEO4J_USER = "neo4j";
	private static final String NEO4J_PASSWORD = "Sl123456";
	private final Driver driver;

	@Resource(name = "decomposeLanguageModel")
	private OllamaChatModel decomposeLanguageModel;

	public GraphService() {
		this.driver = GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
	}

	public void readStory(String path) {
		File storyFile = new File(path);
		List<FileReadUtil.ParseResult> parseResults = FileReadUtil.readEpubFile(storyFile);
		big:for (FileReadUtil.ParseResult parseResult : parseResults) {
			inner:for (int i = 1; i < parseResult.getContentList().size() - 2; i++) {
				DecomposeAssistant assistant = AiServices.builder(DecomposeAssistant.class).chatModel(decomposeLanguageModel).build();
				String decomposeQuery = assistant.enhancedEntityExtraction(parseResult.getContentList().get(i), parseResult.getContentList().get(i - 1), parseResult.getContentList().get(i + 1));
				System.out.println("s = " + decomposeQuery);
				executeCypher(decomposeQuery);
				break big;
			}
		}
	}

	private void executeCypher(String cypher) {
		try (Session session = driver.session()) {
			session.run(cypher);
		} catch (Exception e) {
			System.err.println("❌ Cypher 执行失败：" + cypher);
			e.printStackTrace();
		}
	}
}
