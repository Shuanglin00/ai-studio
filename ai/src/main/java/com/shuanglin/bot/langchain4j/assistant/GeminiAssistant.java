package com.shuanglin.bot.langchain4j.assistant;

import com.google.gson.JsonObject;
import dev.langchain4j.service.*;
/**
 * 封装的Gemini Assistant chat入口
 *
 * @author lin
 * @date 2025/06/27
 */
public interface GeminiAssistant  {
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

	/**
	 *
	 * 聊天
	 * @param memoryId 设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question 原始问题，通过@V注解替换掉user-message.txt中的question变量
	 * @return
	 */
	@UserMessage(value = "{{question}}")
	String chat(@MemoryId JsonObject memoryId,
			@V("question") String question
	);


	@UserMessage(value = """
			{{question}}
			""")
	String groupChat(
			@MemoryId JsonObject memoryId,
			@V("question") String question
	);


	/**
	 * 聊天流式输出，返回TokenStream
	 * @param role 设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question 原始问题，通过@V注解替换掉user-message.txt中的question变量
	 * @param extraInfo 额外信息
	 * @return
	 */
	// 注意：UserMessage会在检索增强时被带入到查询条件中，所以尽量不要放太多无关的文本。如果需要可以在RAG中使用ContentInjector
	TokenStream chatStreamTokenStream(
			@V("role") String role,
			@V("question") String question,
			@V("extraInfo") String extraInfo);

}
