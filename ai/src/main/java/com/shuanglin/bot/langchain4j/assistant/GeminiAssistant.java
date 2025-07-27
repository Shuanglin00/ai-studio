package com.shuanglin.bot.langchain4j.assistant;

import com.google.gson.JsonObject;
import dev.langchain4j.service.*;
import reactor.core.publisher.Flux;
/**
 * 封装的Gemini Assistant chat入口
 *
 * @author lin
 * @date 2025/06/27
 */
public interface GeminiAssistant  {

	/**
	 *
	 * 聊天
	 * @param role 设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question 原始问题，通过@V注解替换掉user-message.txt中的question变量
	 * @return
	 */
	@UserMessage(value = "{{question}}")
	@SystemMessage(value = "使用中文回答,限制返回字数小于200字;")

	String memoryChat(
			@MemoryId String memoryId,
			@V("params") JsonObject params,
			@UserName String userid,
			@V("question") String question);


	@UserMessage(value = "{{question}}")
	String groupChat(
			@MemoryId JsonObject senderInfo,
			@V("question") String question);

	/**
	 * 聊天流式输出
	 * @param sessionId 会话id，通过@MemoryId指定
	 * @param role 设定角色，通过@V注解替换掉system-message.txt中的role变量
	 * @param question 原始问题，通过@V注解替换掉user-message.txt中的question变量
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
	 * @param sessionId 会话id，通过@MemoryId指定
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
