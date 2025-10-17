package com.shuanglin.bot.service;

import com.shuanglin.bot.langchain4j.assistant.DecomposeAssistant;
import com.shuanglin.bot.model.CleanupReport;
import com.shuanglin.bot.model.IsolationMetadata;
import com.shuanglin.bot.model.ProcessReport;
import com.shuanglin.bot.utils.FileReadUtil;
import com.shuanglin.dao.Articles.ArticlesEntity;
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
import org.neo4j.driver.Result;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GraphService {
	private static final String NEO4J_URI = "bolt://8.138.204.38:7687";
	private static final String NEO4J_USER = "neo4j";
	private static final String NEO4J_PASSWORD = "password";
	private final Driver driver;

	@Resource
	private DecomposeAssistant decomposeAssistant;

	@Resource
	private ChapterStorageService chapterStorageService;

	/**
	 * 小说知识图谱构建 - User Prompt
	 * 
	 * 注意：本方法返回的User Prompt将与System Prompt (kgKnowlage.md全文)一同传递给LLM
	 * - System Prompt: 定义本体论框架、通用规则、领域实体设计规范（权威来源）
	 * - User Prompt: 提供任务上下文、具体操作指南、示例演示（引用应用）
	 * 
	 * 版本: v3.1-pure-chapter-context
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
				lastContext（上一章完整内容）：
				{{lastContext}}
				
				作用：确认实体一致性、推断前置状态，**不提取新信息**
				
				---
				
				indexText（当前章完整内容）：
				{{indexText}}
				
				作用：**唯一的信息提取来源**，所有Cypher必须基于此生成
				
				---
				
				nextContext（下一章完整内容）：
				{{nextContext}}
				
				作用：消除歧义、理解语境，**不生成Cypher**
				
				【关键约束】
				- Event.timestamp 必须使用：datetime('{{baseTimestamp}}')
				- Event.source 格式：第{{chapterIndex}}章 {{chapterTitle}}
				- Event节点不包含paragraphIndex属性
				- Event.chapterIndex 设为 {{chapterIndex}}
				
				请严格遵循SystemPrompt的RULE-1至RULE-6 (kgKnowlage.md)，生成符合规范的Cypher语句。
				
				**输出规范：**
				1. ⚠️ **禁止Markdown代码块！** 不允许使用```cypher```或```包裹，直接输出Cypher语句
				2. 禁止输出任何自然语言解释
				3. 如indexText无新信息，必须返回空字符串
				4. 使用MERGE保证幂等性，避免重复创建
				5. 节点标签使用双标签：:Entity:Character, :Event:StoryEvent
				6. 关系类型只能有一个名称，不能使用多标签（如[:RELATION:father]）
				7. **Cypher变量引用规则：**
				   - ❌ 错误: `MERGE (c1:Character {...}) ... MERGE (e)-[:MENTIONS]->(c1 {name: "xxx"})` 
				   - ✅ 正确: `MERGE (c1:Character {...}) ... MERGE (e)-[:MENTIONS]->(c1)`
				   - **说明:** 已声明的变量不能再次添加属性或标签，直接引用变量名即可
				
				**错误示例（绝对禁止）：**
				```cypher
				MERGE (c:Entity:Character {...})
				```
				
				**正确示例：**
				MERGE (c:Entity:Character {...})
				MERGE (e:Event:StoryEvent {...})
				MERGE (c)-[:PARTICIPATED_IN]->(e)
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
			
			// 调用LLM生成Cypher（使用 kgKnowlage.md 作为 System Prompt）
			Prompt prompt = graphPromptTemplate().apply(variables);
			String cypher = decomposeAssistant.generateCypher(prompt.text());
			
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
	 * 清理Markdown代码块标记（LLM可能会错误地添加）
	 * @param cypher 原始Cypher语句
	 * @return 清理后的Cypher语句
	 */
	private String cleanMarkdownCodeBlock(String cypher) {
		if (cypher == null) {
			return null;
		}
		
		String cleaned = cypher.trim();
		
		// 检测并移除开头的```cypher或```
		if (cleaned.startsWith("```cypher")) {
			cleaned = cleaned.substring(9).trim(); // 移除```cypher
			System.err.println("⚠️  检测到Markdown代码块标记（```cypher），已自动清理");
		} else if (cleaned.startsWith("```")) {
			cleaned = cleaned.substring(3).trim(); // 移除```
			System.err.println("⚠️  检测到Markdown代码块标记（```），已自动清理");
		}
		
		// 移除结尾的```
		if (cleaned.endsWith("```")) {
			cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
		}
		
		return cleaned;
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
		
		// 验证Event节点不应包含paragraphIndex属性（章节级处理）
		if (cypher.contains("paragraphIndex:")) {
			System.err.println("⚠️  验证失败：Event节点不应包含paragraphIndex属性（章节级处理）");
			return false;
		}
		
		// 验证timestamp格式为YYYY-MM-DDT00:00:00（日期级精度）
		if (cypher.contains("timestamp:") && !cypher.matches(".*datetime\\('\\d{4}-\\d{2}-\\d{2}T00:00:00'\\).*")) {
			System.err.println("⚠️  验证警告：timestamp格式应为YYYY-MM-DDT00:00:00（日期级精度）");
			// 警告但不阻断执行（容错处理）
		}
		
		// 验证source格式为"第X章 章节名"（移除段落标记）
		if (cypher.contains("source:") && cypher.contains(" - P")) {
			System.err.println("⚠️  验证失败：source格式应为'第X章 章节名'，不应包含段落标记");
			return false;
		}
		
		// 验证变量重复声明（检测已声明变量再次添加属性的错误模式）
		// 错误模式: MERGE (c1:...) ... MERGE (e)-[:REL]->(c1 {name: "xxx"})
		Pattern varWithPropsPattern = Pattern.compile("-\\[:.*?]-\\>\\s*\\((\\w+)\\s*\\{[^}]+\\}");
		Matcher varWithPropsMatcher = varWithPropsPattern.matcher(cypher);
		
		while (varWithPropsMatcher.find()) {
			String varNameInRel = varWithPropsMatcher.group(1);
			// 检测该变量是否已经通过MERGE或CREATE声明
			Pattern varDeclPattern = Pattern.compile("(MERGE|CREATE)\\s*\\(" + varNameInRel + "[:(");
			Matcher varDeclMatcher = varDeclPattern.matcher(cypher);
			
			if (varDeclMatcher.find()) {
				System.err.println("⚠️  验证失败：变量 '" + varNameInRel + "' 已经声明，不能在关系中再次添加属性");
				System.err.println("❌ 错误模式: MERGE/CREATE ("+varNameInRel+":...) ... MERGE (e)-[:REL]->("+varNameInRel+" {props})");
				System.err.println("✅ 正确写法: MERGE/CREATE ("+varNameInRel+":...) ... MERGE (e)-[:REL]->("+varNameInRel+")");
				return false;
			}
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

	// ========== 新增：章节限制读取与知识图谱构建 ==========

	/**
	 * 章节限制读取并构建知识图谱（含MongoDB持久化）
	 * @param path EPUB文件路径
	 * @param chapterLimit 章节数量限制
	 * @param metadata 数据隔离元数据
	 * @return 处理报告
	 */
	public ProcessReport readStoryWithLimit(String path, int chapterLimit, IsolationMetadata metadata) {
		// 验证元数据
		metadata.validate();
		
		long startTime = System.currentTimeMillis();
		ProcessReport report = new ProcessReport(metadata.getBookUuid(), metadata.getBookName());
		
		File storyFile = new File(path);
		List<FileReadUtil.ParseResult> parseResults = FileReadUtil.readEpubFile(storyFile);
		
		// 限制章节数量
		int actualLimit = Math.min(parseResults.size(), chapterLimit);
		report.setTotalChapters(actualLimit);
		
		if (parseResults.size() < chapterLimit) {
			System.out.println("⚠️  EPUB文件仅包含 " + parseResults.size() + " 章节，少于请求的 " + chapterLimit + " 章节");
		}
		
		System.out.println("📚 开始处理《" + metadata.getBookName() + "》前 " + actualLimit + " 个章节...");
		
		// 遍历每个章节（章节级循环）
		for (int chapterIdx = 0; chapterIdx < actualLimit; chapterIdx++) {
			long chapterStartTime = System.currentTimeMillis();
			
			FileReadUtil.ParseResult currentChapter = parseResults.get(chapterIdx);
			
			// 步骤A: 准备上下文
			String lastChapterText = chapterIdx > 0 
					? aggregateParagraphs(parseResults.get(chapterIdx - 1).getContentList()) 
					: "";
			String currentChapterText = aggregateParagraphs(currentChapter.getContentList());
			String nextChapterText = chapterIdx < parseResults.size() - 1 
					? aggregateParagraphs(parseResults.get(chapterIdx + 1).getContentList()) 
					: "";
			
			// 步骤B: 构造元数据
			String chapterTitle = currentChapter.getTitle();
			int chapterIndex = chapterIdx + 1; // 从1开始
			String baseTimestamp = calculateTimestamp(chapterIndex);
			
			// 步骤C: 构建 ArticlesEntity（初始状态）
			ArticlesEntity entity = buildArticlesEntity(
					currentChapterText,
					chapterTitle,
					chapterIndex,
					currentChapter.getContentList().size(),
					metadata
			);
			
			// 步骤D: 先保存章节内容到MongoDB
			ArticlesEntity savedEntity = chapterStorageService.saveChapterWithCypher(entity);
			String docId = savedEntity.getId();
			
			// 步骤E: 构造 Prompt 变量
			Map<String, Object> variables = new HashMap<>();
			variables.put("lastContext", lastChapterText);
			variables.put("indexText", currentChapterText);
			variables.put("nextContext", nextChapterText);
			variables.put("chapterTitle", chapterTitle);
			variables.put("chapterIndex", chapterIndex);
			variables.put("baseTimestamp", baseTimestamp);
			
			// 步骤F: 调用LLM生成Cypher（使用 kgKnowlage.md 作为 System Prompt）
			Prompt prompt = graphPromptTemplate().apply(variables);
			String promptText = prompt.text();
			
			// 调试：打印 prompt 内容（仅第一次）
			if (chapterIndex == 1) {
				System.out.println("\n=== DEBUG: Prompt Text (Chapter 1) ===");
				System.out.println(promptText.substring(0, Math.min(500, promptText.length())));
				System.out.println("... (truncated)\n");
			}
			
			String cypher = null;
			try {
				cypher = decomposeAssistant.generateCypher(promptText);
				
				// 清理Markdown代码块标记（如果LLM错误地添加了）
				cypher = cleanMarkdownCodeBlock(cypher);
				
				// 步骤G: 更新 MongoDB 的 cypherStatements
				savedEntity.setCypherStatements(cypher);
				savedEntity.setProcessStatus("PROCESSING");
				chapterStorageService.updateCypherContent(savedEntity);
				
			} catch (Exception e) {
				System.err.println("❌ 章节 " + chapterIndex + " LLM调用失败: " + e.getMessage());
				chapterStorageService.updateCypherExecuteStatus(docId, "FAILED", "LLM调用失败: " + e.getMessage(), Instant.now().toString());
				report.setFailedChapters(report.getFailedChapters() + 1);
				continue;
			}
			
			// 步骤H: 验证Cypher
			if (!validate(cypher)) {
				System.err.println("⚠️  章节 " + chapterIndex + " 验证失败，跳过执行");
				chapterStorageService.updateCypherExecuteStatus(docId, "FAILED", "Cypher验证失败", Instant.now().toString());
				report.setSkippedChapters(report.getSkippedChapters() + 1);
				continue;
			}
			
			// 步骤I: 注入元数据
			String enhancedCypher = injectMetadata(cypher, metadata, docId);
			
			// 步骤J: 执行Cypher到Neo4j
			boolean executeSuccess = false;
			String errorMsg = null;
			try {
				executeBatchCypher(enhancedCypher);
				executeSuccess = true;
				System.out.println("✅ 已处理章节 " + chapterIndex + "/" + actualLimit + ": " + chapterTitle + " (耗时: " + (System.currentTimeMillis() - chapterStartTime) + "ms)");
				
			} catch (Exception e) {
				errorMsg = e.getMessage();
				System.err.println("❌ 章节 " + chapterIndex + " Cypher执行失败: " + errorMsg);
			}
			
			// 步骤K: 更新执行状态
			if (executeSuccess) {
				chapterStorageService.updateCypherExecuteStatus(docId, "SUCCESS", null, Instant.now().toString());
				report.setSuccessChapters(report.getSuccessChapters() + 1);
			} else {
				chapterStorageService.updateCypherExecuteStatus(docId, "FAILED", errorMsg, Instant.now().toString());
				report.setFailedChapters(report.getFailedChapters() + 1);
			}
		}
		
		long endTime = System.currentTimeMillis();
		report.setTotalDuration(endTime - startTime);
		report.setAvgChapterDuration(report.getTotalChapters() > 0 ? report.getTotalDuration() / report.getTotalChapters() : 0L);
		
		System.out.println("\n📊 知识图谱构建完成！共处理 " + actualLimit + " 个章节");
		System.out.println(report);
		
		return report;
	}

	/**
	 * 构建 ArticlesEntity 对象
	 * @param content 章节内容
	 * @param title 章节标题
	 * @param chapterIndex 章节索引
	 * @param paragraphCount 段落数
	 * @param metadata 隔离元数据
	 * @return ArticlesEntity 对象
	 */
	private ArticlesEntity buildArticlesEntity(String content, String title, int chapterIndex, 
											   int paragraphCount, IsolationMetadata metadata) {
		return ArticlesEntity.builder()
				.id(UUID.randomUUID().toString())
				.title(title)
				.content(content)
				.bookUuid(metadata.getBookUuid())
				.chapterIndex(chapterIndex)
				.processStatus("PENDING")
				.cypherExecuteStatus("PENDING")
				.paragraphCount(paragraphCount)
				.dataSource(metadata.getDataSource())
				.createTime(Instant.now().toString())
				.tags(buildTags(metadata))
				.build();
	}

	/**
	 * 构建标签字符串（JSON格式）
	 * @param metadata 隔离元数据
	 * @return JSON字符串
	 */
	private String buildTags(IsolationMetadata metadata) {
		return "{\"bookName\":\"" + metadata.getBookName() + 
			   "\",\"dataSource\":\"" + metadata.getDataSource() + "\"}";
	}

	/**
	 * 向Cypher注入隔离元数据
	 * @param cypher 原始Cypher语句
	 * @param metadata 隔离元数据
	 * @param mongoDocId MongoDB文档ID
	 * @return 注入后的Cypher语句
	 */
	private String injectMetadata(String cypher, IsolationMetadata metadata, String mongoDocId) {
		if (cypher == null || cypher.trim().isEmpty()) {
			return cypher;
		}
		
		// 注入属性
		String injectedProps = String.format(
				"dataSource: '%s', bookName: '%s', bookUuid: '%s', mongoDocId: '%s'",
				metadata.getDataSource(),
				metadata.getBookName(),
				metadata.getBookUuid(),
				mongoDocId
		);
		
		// 正则匹配节点创建语句并注入属性
		// 匹配 CREATE (var:Label {props}) 或 MERGE (var:Label {props})
		Pattern pattern = Pattern.compile("(CREATE|MERGE)\\s+\\(([^)]+)\\{([^}]*)\\}\\)");
		Matcher matcher = pattern.matcher(cypher);
		
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String operation = matcher.group(1); // CREATE or MERGE
			String nodePattern = matcher.group(2); // var:Label
			String existingProps = matcher.group(3); // existing properties
			
			// 构造新的属性（追加注入属性）
			String newProps = existingProps.trim().isEmpty() 
					? injectedProps 
					: existingProps + ", " + injectedProps;
			
			// 替换为增强后的节点创建语句
			String replacement = operation + " (" + nodePattern + "{" + newProps + "})";
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		
		return result.toString();
	}

	/**
	 * 从MongoDB重放Cypher到Neo4j
	 * @param bookUuid 书籍唯一标识
	 * @param chapterIndex 章节索引
	 * @return 是否成功
	 */
	public boolean replayCypherFromMongo(String bookUuid, Integer chapterIndex) {
		ArticlesEntity entity = chapterStorageService.queryChapterByIndex(bookUuid, chapterIndex);
		if (entity == null) {
			System.err.println("❌ 未找到章节: " + bookUuid + ", 章节" + chapterIndex);
			return false;
		}
		
		String cypher = entity.getCypherStatements();
		if (cypher == null || cypher.trim().isEmpty()) {
			System.err.println("❌ 章节 " + chapterIndex + " 无Cypher语句可重放");
			return false;
		}
		
		// 清理Markdown代码块标记
		cypher = cleanMarkdownCodeBlock(cypher);
		
		// 验证Cypher
		if (!validate(cypher)) {
			System.err.println("⚠️  章节 " + chapterIndex + " Cypher验证失败，跳过重放");
			return false;
		}
		
		// 执行到Neo4j
		try {
			executeBatchCypher(cypher);
			chapterStorageService.updateCypherExecuteStatus(entity.getId(), "SUCCESS", null, Instant.now().toString());
			System.out.println("✅ 章节 " + chapterIndex + " Cypher重放成功");
			return true;
			
		} catch (Exception e) {
			String errorMsg = e.getMessage();
			chapterStorageService.updateCypherExecuteStatus(entity.getId(), "FAILED", errorMsg, Instant.now().toString());
			System.err.println("❌ 章节 " + chapterIndex + " Cypher重放失败: " + errorMsg);
			return false;
		}
	}

	/**
	 * 清理测试数据
	 * @param dataSource 数据源标识
	 * @return 清理报告
	 */
	public CleanupReport cleanupTestData(String dataSource) {
		long startTime = System.currentTimeMillis();
		CleanupReport report = new CleanupReport(dataSource);
		
		System.out.println("🧹 开始清理数据源: " + dataSource);
		
		// 清理Neo4j数据
		try (Session session = driver.session()) {
			// 删除所有匹配dataSource的节点和关系
			String deleteCypher = "MATCH (n {dataSource: '" + dataSource + "'}) DETACH DELETE n";
			Result result = session.run(deleteCypher);
			
			// 获取删除统计（简化处理）
			System.out.println("✅ Neo4j数据已清理");
			
		} catch (Exception e) {
			System.err.println("❌ Neo4j清理失败: " + e.getMessage());
		}
		
		// 清理MongoDB数据
		try {
			Long deletedCount = chapterStorageService.queryChaptersByBook(dataSource).stream()
					.filter(e -> dataSource.equals(e.getDataSource()))
					.count();
			
			// 删除所有匹配dataSource的文档
			chapterStorageService.queryChaptersByBook(dataSource).stream()
					.filter(e -> dataSource.equals(e.getDataSource()))
					.forEach(e -> chapterStorageService.deleteBookData(e.getBookUuid()));
			
			report.setMongoDocsDeleted(deletedCount);
			System.out.println("✅ MongoDB数据已清理，删除 " + deletedCount + " 个文档");
			
		} catch (Exception e) {
			System.err.println("❌ MongoDB清理失败: " + e.getMessage());
		}
		
		long endTime = System.currentTimeMillis();
		report.setCleanupDuration(endTime - startTime);
		
		System.out.println(report);
		return report;
	}

	/**
	 * 查询测试数据统计信息
	 * @param dataSource 数据源标识
	 * @return 统计信息
	 */
	public String queryTestDataStats(String dataSource) {
		// 查询MongoDB统计
		List<ArticlesEntity> chapters = chapterStorageService.queryChaptersByBook(dataSource).stream()
				.filter(e -> dataSource.equals(e.getDataSource()))
				.toList();
		
		long totalChapters = chapters.size();
		long completedChapters = chapters.stream().filter(e -> "COMPLETED".equals(e.getProcessStatus())).count();
		long failedChapters = chapters.stream().filter(e -> "FAILED".equals(e.getProcessStatus())).count();
		long pendingChapters = chapters.stream().filter(e -> "PENDING".equals(e.getProcessStatus())).count();
		
		// 查询Neo4j统计
		int entityCount = 0;
		int eventCount = 0;
		int stateCount = 0;
		
		try (Session session = driver.session()) {
			// 统计Entity节点
			Result entityResult = session.run(
					"MATCH (n:Entity {dataSource: '" + dataSource + "'}) RETURN count(n) as count"
			);
			if (entityResult.hasNext()) {
				entityCount = entityResult.next().get("count").asInt();
			}
			
			// 统计Event节点
			Result eventResult = session.run(
					"MATCH (n:Event {dataSource: '" + dataSource + "'}) RETURN count(n) as count"
			);
			if (eventResult.hasNext()) {
				eventCount = eventResult.next().get("count").asInt();
			}
			
			// 统计State节点
			Result stateResult = session.run(
					"MATCH (n:State {dataSource: '" + dataSource + "'}) RETURN count(n) as count"
			);
			if (stateResult.hasNext()) {
				stateCount = stateResult.next().get("count").asInt();
			}
			
		} catch (Exception e) {
			System.err.println("❌ Neo4j统计查询失败: " + e.getMessage());
		}
		
		String stats = "\n📊 数据统计报告\n" +
				"========================================\n" +
				"数据源: " + dataSource + "\n" +
				"MongoDB 统计:\n" +
				"  总章节数: " + totalChapters + "\n" +
				"  已完成: " + completedChapters + "\n" +
				"  失败: " + failedChapters + "\n" +
				"  待处理: " + pendingChapters + "\n" +
				"Neo4j 统计:\n" +
				"  Entity节点数: " + entityCount + "\n" +
				"  Event节点数: " + eventCount + "\n" +
				"  State节点数: " + stateCount + "\n" +
				"========================================";
		
		System.out.println(stats);
		return stats;
	}
}
