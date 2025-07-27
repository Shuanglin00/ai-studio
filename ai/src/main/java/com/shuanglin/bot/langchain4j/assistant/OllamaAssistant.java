package com.shuanglin.bot.langchain4j.assistant;

import com.google.gson.JsonObject;
import dev.langchain4j.service.*;
import reactor.core.publisher.Flux;

public interface OllamaAssistant {

	@UserMessage(value = """
			# 首先基础原则
			1. 你必须遵守中华人民共和国法律法规，不得逾越或触碰任何违法甚至损害中国形象。
			2. 你必须使用简体中文，或者繁体中文，或者粤语的俚语进行回去，取决于问题所使用语言。
			3. 你将扮演多个角色，回答符合角色设定且根据历史记录相关的回答。
			4. 回答内容尽可能符合角色设定，字数保持在200以内。
			---
			# 角色
			{{role}}
			# 角色设定
			{{description}}
			
			---\s
			# 行为指令
			{{instruction}}
			
			---
			# 当前用户需求
			{{question}}
			
			""")
	String groupChat(
			@MemoryId JsonObject senderInfo,
			@V("question") String question,
			@V("instruction") String instruction,
			@V("description") String description,
			@V("role") String role
	);

	/**
	 * 聊天
	 *
	 * @param role     设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question 原始问题，通过@V注解替换掉user-message.txt中的question变量
	 * @return
	 */
	@UserMessage(value = "{{question}}")
	@SystemMessage(value = "使用中文回答,限制返回字数小于200字")
	String chat(
			@MemoryId String memoryId,
			@V("role") String role,
			@UserName String userid,
			@V("question") String question);

	/**
	 * 聊天流式输出
	 *
	 * @param sessionId 会话id，通过@MemoryId指定
	 * @param role      设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question  原始问题，通过@V注解替换掉user-message.txt中的question变量
	 * @param extraInfo 额外信息
	 * @return
	 */
	@SystemMessage(fromResource = "prompt/system-message.txt")
	@UserMessage(fromResource = "prompt/user-message.txt")
	Flux<String> chatStreamFlux(
			@V("role") String role,
			@V("question") String question,
			@V("extraInfo") String extraInfo);

	/**
	 * 聊天流式输出，返回TokenStream
	 *
	 * @param sessionId 会话id，通过@MemoryId指定
	 * @param role      设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question  原始问题，通过@V注解替换掉user-message.txt中的question变量
	 * @param extraInfo 额外信息
	 * @return
	 */
	// 注意：UserMessage会在检索增强时被带入到查询条件中，所以尽量不要放太多无关的文本。如果需要可以在RAG中使用ContentInjector
	TokenStream chatStreamTokenStream(
			@V("role") String role,
			@V("question") String question,
			@V("extraInfo") String extraInfo);
}
