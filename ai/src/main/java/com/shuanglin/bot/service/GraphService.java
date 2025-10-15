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

	/**
	 * 小说知识图谱构建 - User Prompt
	 * 
	 * 注意：本方法返回的User Prompt将与System Prompt (kgKnowlage.md全文)一同传递给LLM
	 * - System Prompt: 定义本体论框架、通用规则、领域实体设计规范（权威来源）
	 * - User Prompt: 提供任务上下文、具体操作指南、示例演示（引用应用）
	 * 
	 * 版本: v2.0-domain-unified
	 * 配套System Prompt: kgKnowlage.md 第6.5节 - 领域实体设计规范
	 */
	public PromptTemplate graphPromptTemplate() {
		return PromptTemplate.from("""
				/**
				 * 小说知识图谱增量构建任务 - User Prompt
				 * 版本: v2.0-domain-unified
				 * 配套System Prompt: kgKnowlage.md (完整文档已作为system message传递)
				 * 
				 * 重要声明: 本指令与 System Prompt 协同工作，不是替换关系。
				 * System Prompt 提供本体论框架和规范，本指令提供任务上下文和操作指南。
				 */
				
				你是高度专业化的知识图谱构建AI，擅长从大规模小说文本中进行信息抽取和结构化建模。
				你的任务是将章节内容精确、高效地转换为Neo4j Cypher语句，用于构建或扩展小说世界知识图谱。
				
				**上下文信息 (Context):**
				你将接收到以下三部分文本，这是你分析的全部依据：
				-   `lastContext`: 前一章节的全部内容。用于**实体识别的一致性**、**关系回溯**和**状态变迁的验证**。
				    你需要利用它来理解已知角色的背景和现有关系，但**不应**从中提取新的信息来生成Cypher。
				-   `indexText`: 当前需要处理的完整章节内容。这是你本次任务的**核心工作区**。
				    你必须通读并理解本章节的全部情节，并从中提取所有新增的、变化的实体、关系和属性。
				-   `nextContext`: 下一章节的全部内容。用于**预判和理解提供线索**，帮助消除歧义。
				    但同样**不应**从中提取新的信息来生成Cypher。
				
				**核心任务 (Core Mission):**
				你的使命是"增量更新"知识图谱。基于`lastContext`和`nextContext`提供的上下文，
				对`indexText`(当前章节)进行一次全面的信息抽取，并生成一系列Cypher语句来反映**本章节带来的所有新变化**。
				
				**必须严格遵循System Prompt (kgKnowlage.md) 第6.5节定义的领域实体设计规范**
				
				**生成要求:**
				1. 纯Cypher输出，禁止Markdown/注释/自然语言
				2. 使用MERGE保证幂等性
				3. 节点标签使用双标签：[:Entity:Character], [:Event:StoryEvent]
				4. 易变属性必须通过:State节点管理
				5. 状态变迁遵循System Prompt第6.3节原子化模板
				6. 如无新信息，返回空字符串
				
				请处理用户提供的章节内容。
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
