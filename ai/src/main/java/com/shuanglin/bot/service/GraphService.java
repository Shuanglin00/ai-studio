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
	 * 版本: v3.0-chapter-level
	 * 配套System Prompt: kgKnowlage.md 第6.5节 - 领域实体设计规范
	 */
	public PromptTemplate graphPromptTemplate() {
		return PromptTemplate.from("""
				## 当前任务
				请基于SystemPrompt中定义的强制性约束规则，处理以下输入：
				
				【章节信息】
				- 章节标题：{{chapterTitle}}
				- 章节索引：{{chapterIndex}}
				- 基准时间戳：{{baseTimestamp}}
				
				【文本内容】
				lastContext（前一章节/完整内容）：
				{{lastContext}}
				
				作用：确认实体一致性、推断前置状态，**不提取新信息**
				
				---
				
				indexText（当前章节/完整内容）：
				{{indexText}}
				
				作用：**唯一的信息提取来源**，所有Cypher必须基于此生成
				
				---
				
				nextContext（下一章节/完整内容）：
				{{nextContext}}
				
				作用：消除歧义、理解语境，**不生成Cypher**
				
				【关键约束】
				- Event.timestamp 必须使用：datetime('{{baseTimestamp}}')
				- Event.source 格式：第{{chapterIndex}}章 {{chapterTitle}}
				- Event.paragraphIndex 设为 null
				- Event.chapterIndex 设为 {{chapterIndex}}
				
				请严格遵循SystemPrompt的RULE-1至RULE-6 (kgKnowlage.md)，生成符合规范的Cypher语句。
				
				**输出规范：**
				1. 直接输出Cypher语句，禁止Markdown代码块包裹
				2. 禁止输出任何自然语言解释
				3. 如indexText无新信息，必须返回空字符串
				4. 使用MERGE保证幂等性，避免重复创建
				5. 节点标签使用双标签：[:Entity:Character], [:Event:StoryEvent]
				"""
		);
	}

	public GraphService() {
		this.driver = GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
	}

	/**
	 * 章节级小说知识图谱构建
	 * 以完整章节为处理单位，每章调用1次LLM
	 */
	public void readStory(String path) {
		File storyFile = new File(path);
		List<FileReadUtil.ParseResult> parseResults = FileReadUtil.readEpubFile(storyFile);
		
		// 遍历每个章节（章节级循环）
		for (int chapterIdx = 0; chapterIdx < parseResults.size(); chapterIdx++) {
			FileReadUtil.ParseResult currentChapter = parseResults.get(chapterIdx);
			
			// 聚合段落为完整章节文本
			String lastChapterText = chapterIdx > 0 
					? aggregateParagraphs(parseResults.get(chapterIdx - 1).getContentList()) 
					: "";
			String currentChapterText = aggregateParagraphs(currentChapter.getContentList());
			String nextChapterText = chapterIdx < parseResults.size() - 1 
					? aggregateParagraphs(parseResults.get(chapterIdx + 1).getContentList()) 
					: "";
			
			// 构造章节元数据
			String chapterTitle = currentChapter.getTitle();
			int chapterIndex = chapterIdx + 1; // 从1开始
			String baseTimestamp = calculateTimestamp(chapterIndex);
			
			// 构造Prompt变量
			Map<String, Object> variables = new HashMap<>();
			variables.put("lastContext", lastChapterText);
			variables.put("indexText", currentChapterText);
			variables.put("nextContext", nextChapterText);
			variables.put("chapterTitle", chapterTitle);
			variables.put("chapterIndex", chapterIndex);
			variables.put("baseTimestamp", baseTimestamp);
			
			// 调用LLM生成Cypher
			Prompt prompt = graphPromptTemplate().apply(variables);
			String cypher = decomposeLanguageModel.chat(prompt.text());
			
			// 验证并执行Cypher
			if (validate(cypher)) {
				executeBatchCypher(cypher);
				System.out.println("✅ 已处理章节 " + chapterIndex + "/" + parseResults.size() + ": " + chapterTitle);
			} else {
				System.err.println("⚠️  章节 " + chapterIndex + " 验证失败，跳过执行");
			}
		}
		
		System.out.println("\n📊 知识图谱构建完成！共处理 " + parseResults.size() + " 个章节");
	}
	
	/**
	 * 聚合段落列表为完整章节文本
	 * @param contentList 章节的段落列表
	 * @return 聚合后的完整文本
	 */
	private String aggregateParagraphs(List<String> contentList) {
		if (contentList == null || contentList.isEmpty()) {
			return "";
		}
		
		return contentList.stream()
				.filter(paragraph -> paragraph != null && !paragraph.trim().isEmpty())
				.reduce((p1, p2) -> p1 + "\n" + p2)
				.orElse("");
	}
	
	/**
	 * 计算章节的基准时间戳（日期级精度）
	 * @param chapterIndex 章节索引（从1开始）
	 * @return ISO 8601格式的时间戳字符串
	 */
	private String calculateTimestamp(int chapterIndex) {
		// 基准日期：2025-01-01
		// 公式：baseDate + (chapterIndex * 1天)
		return String.format("2025-01-%02dT00:00:00", chapterIndex);
	}
	
	/**
	 * 验证Cypher语句的本体约束
	 * @param cypher Cypher语句
	 * @return 是否通过验证
	 */
	private boolean validate(String cypher) {
		if (cypher == null || cypher.trim().isEmpty()) {
			return false; // 空语句跳过
		}
		
		// 验证Event.paragraphIndex为null（章节级处理不使用paragraphIndex）
		if (cypher.contains("paragraphIndex:") && !cypher.contains("paragraphIndex: null")) {
			System.err.println("⚠️  验证失败：Event.paragraphIndex必须设为null（章节级处理）");
			return false;
		}
		
		// 验证timestamp格式为YYYY-MM-DDT00:00:00（日期级精度）
		if (cypher.contains("timestamp:") && !cypher.matches(".*datetime\\('\\d{4}-\\d{2}-\\d{2}T00:00:00'\\).*")) {
			System.err.println("⚠️  验证失败：timestamp格式必须为YYYY-MM-DDT00:00:00");
			// 警告但不阻断执行（容错处理）
		}
		
		// 验证source格式为"第X章 章节名"（移除段落标记）
		if (cypher.contains("source:") && cypher.contains(" - P")) {
			System.err.println("⚠️  验证警告：source格式应为'第X章 章节名'，不应包含段落标记");
			// 警告但不阻断执行
		}
		
		return true; // 通过验证
	}
	
	/**
	 * 批量执行Cypher语句，支持事务和回滚
	 * @param cypher Cypher语句
	 */
	private void executeBatchCypher(String cypher) {
		try (Session session = driver.session()) {
			// 分离多条CREATE/MERGE语句（简单处理）
			String[] statements = cypher.split(";\\s*(?=CREATE|MERGE|MATCH)");
			
			// 开启事务
			session.writeTransaction(tx -> {
				for (String statement : statements) {
					if (statement != null && !statement.trim().isEmpty()) {
						try {
							tx.run(statement.trim());
						} catch (Exception e) {
							System.err.println("❌ 单条语句执行失败：" + statement.trim());
							e.printStackTrace();
							throw e; // 抛出异常触发事务回滚
						}
					}
				}
				return null;
			});
			
			System.out.println("✅ 批量执行成功，共 " + statements.length + " 条语句");
			
		} catch (Exception e) {
			System.err.println("❌ 批量Cypher执行失败，事务已回滚");
			System.err.println("原始语句：" + cypher.substring(0, Math.min(cypher.length(), 200)) + "...");
			e.printStackTrace();
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
