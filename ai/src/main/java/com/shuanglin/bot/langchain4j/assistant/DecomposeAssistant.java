package com.shuanglin.bot.langchain4j.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DecomposeAssistant {

	@UserMessage("将以下问题分解为3个或更少的、更简单的子问题。只返回一个以；分号分隔的子问题列表，不要添加任何其他文本。问题：{{query}}")
	String decompose(@V("query") String query);

	/**
	 * 通用知识图谱生成方法，使用 kgKnowlage.md 作为 System Prompt
	 * @param userPrompt 用户提示词（任务上下文、操作指南、示例）- 已完成变量替换的完整文本
	 * @return Cypher语句
	 */
	String generateCypher(String userPrompt);
	
	@UserMessage("""
			你是一个知识图谱构建助手，专门用于将小说文本转换为 Neo4j Cypher 插入语句。
			
			**上下文信息：**
			上一章完整内容：　　{{lastContext}}
			当前章完整内容：　　{{indexText}}
			下一章完整内容：　　{{nextContext}}
			
			**任务说明：**
			请基于完整的上下文信息，分析当前章的内容，提取其中的：
			1. 实体（人物、地点、物品、技能、状态等）
			2. 实体间的关系
			3. 实体的属性
			
			**生成要求：**
			1. 只生成 Neo4j Cypher 语句，不添加任何解释
			2. 使用 MERGE 避免重复创建节点和关系
			3. 节点标签：:Character, :Location, :Item, :Skill, :State, :Event
			4. 关系类型：使用英文大写（如 :LOCATED_IN, :USES, :LEARNS, :HAS, :CONTAINS）
			5. 属性使用中文键名（name, 描述, 等级 等）
			6. 如果当前章没有可提取的新信息，返回空字符串
			
			**示例输出格式：**
			MERGE (c:Character {name: "萧炎"})\s
			MERGE (s:State {name: "四段斗之气"})\s
			MERGE (c)-[:HAS_STATE]->(s)
			
			请开始生成：
			""")
	String enhancedEntityExtraction(@V("indexText") String indexText, @V("lastContext") String lastContext, @V("nextContext") String nextContext);
}
