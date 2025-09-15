package com.shuanglin.bot.service;

import com.shuanglin.bot.utils.FileReadUtil;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import jakarta.annotation.Resource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphService {
	private static final String NEO4J_URI = "bolt://8.138.204.38:7687";
	private static final String NEO4J_USER = "neo4j";
	private static final String NEO4J_PASSWORD = "Sl123456";
	private final Driver driver;

	@Resource(name = "decomposeLanguageModel")
	private OllamaChatModel decomposeLanguageModel;

	public PromptTemplate graphPromptTemplate() {
		return PromptTemplate.from("""
				你是一个知识图谱构建助手，专门用于将小说文本转换为 Neo4j Cypher 插入语句。
				
				**上下文信息：**
				前文：　　lastContext
				当前行：　indexText
				后文：　　nextContext
				
				**任务说明：**
				请基于完整的上下文信息，分析当前行的内容，提取其中的：
				1. 实体（人物、地点、物品、技能、状态等）
				2. 实体间的关系
				3. 实体的属性
				
				**生成要求：**
				1. 只生成 Neo4j Cypher 语句，不添加任何解释
				2. 使用 MERGE 避免重复创建节点和关系
				3. 节点标签：:Character, :Location, :Item, :Skill, :State, :Event
				4. 关系类型：使用英文大写（如 :LOCATED_IN, :USES, :LEARNS, :HAS, :CONTAINS）
				5. 属性使用中文键名（name, 描述, 等级 等）
				6. 如果当前行没有可提取的新信息，返回空字符串
				
				**示例输出格式：**
				MERGE (c:Character {name: "萧炎"})\s
				MERGE (s:State {name: "四段斗之气"})\s
				MERGE (c)-[:HAS_STATE]->(s)
				
				请开始生成：
				"""
		);
	}

	public GraphService() {
		this.driver = GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
	}

	public void readStory(String path) {
		File storyFile = new File(path);
		List<FileReadUtil.ParseResult> parseResults = FileReadUtil.readEpubFile(storyFile);
		big:for (FileReadUtil.ParseResult parseResult : parseResults) {
			inner:for (int i = 1; i < parseResult.getContentList().size() - 2; i++) {
				Map<String, String> map = new HashMap<>();
				map.put("lastContext", parseResult.getContentList().get(i - 1));
				map.put("indexText", parseResult.getContentList().get(i));
				map.put("nextContext", parseResult.getContentList().get(i + 1));
				String replace = graphPromptTemplate().template()
						.replace("lastContext", parseResult.getContentList().get(i - 1))
						.replace("indexText", parseResult.getContentList().get(i))
						.replace("nextContext", parseResult.getContentList().get(i + 1));
				String decomposeQuery = decomposeLanguageModel.chat(replace);
				executeCypher(decomposeQuery);
				System.out.println("decomposeQuery = " + decomposeQuery);
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
