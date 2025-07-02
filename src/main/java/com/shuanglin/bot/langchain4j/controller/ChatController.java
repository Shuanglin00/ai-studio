package com.shuanglin.bot.langchain4j.controller;

import com.google.gson.Gson;
import com.shuanglin.bot.langchain4j.assistant.GeminiAssistant;
import dev.langchain4j.service.TokenStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
@RequestMapping("/chat")
public class ChatController {

	@Autowired
	private GeminiAssistant geminiAssistant;

	@GetMapping("/ask")
	public String ask(
	                  @RequestParam(value = "role", required = false, defaultValue = "智能问答助手") String role,
	                  @RequestParam(value = "memoryId", required = false, defaultValue = "智能问答助手") String memoryId,
	                  @RequestParam(value = "question") String question){
		// 日志入口
		System.out.println("[Controller] userId=" + memoryId + ", question=" + question);
		String answer= geminiAssistant.chat(memoryId,role, question);
		System.out.println("[Controller] userId=" + memoryId + ", answer=" + answer);
		return answer;
	}

	/**
	 * 流式聊天
	 *
	 * @param sessionId       会话id
	 * @param role            设定角色
	 * @param question        原始问题
	 * @param webSearchEnable 是否开启网页搜索
	 * @param extraInfo       额外信息（暂未实现）
	 * @return
	 */
	@GetMapping(value = "/stream/flux", produces = TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chatStreamFlux(@RequestParam(value = "sessionId") String sessionId,
	                                   @RequestParam(value = "role", required = false, defaultValue = "智能问答助手") String role,
	                                   @RequestParam(value = "question") String question,
	                                   @RequestParam(value = "webSearchEnable", required = false, defaultValue = "false") Boolean webSearchEnable,
	                                   @RequestParam(value = "extraInfo", required = false, defaultValue = "") String extraInfo) {
		return geminiAssistant.chatStreamFlux(role, question, extraInfo);
	}

	/**
	 * 流式聊天（SSE），方便前端根据KEY渲染不同的内容
	 *
	 * @param sessionId       会话id
	 * @param role            设定角色
	 * @param question        原始问题
	 * @param webSearchEnable 是否开启网页搜索
	 * @param extraInfo       额外信息（暂未实现）
	 * @return
	 */
	@GetMapping(value = "/stream/sse", produces = TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> chatStreamSse(@RequestParam(value = "sessionId") String sessionId,
	                                                   @RequestParam(value = "role", required = false, defaultValue = "智能问答助手") String role,
	                                                   @RequestParam(value = "question") String question,
	                                                   @RequestParam(value = "webSearchEnable", required = false, defaultValue = "false") Boolean webSearchEnable,
	                                                   @RequestParam(value = "extraInfo", required = false, defaultValue = "") String extraInfo) {
		//参考的源码里的写法
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

		TokenStream tokenStream = geminiAssistant.chatStreamTokenStream( role, question, extraInfo);
		//rag回调
		tokenStream.onRetrieved(contents ->
				//前端可监听Retrieved时间，展示命中的文件
				sink.tryEmitNext(ServerSentEvent.builder(new Gson().toJson(contents)).event("Retrieved").build()));
		//消息片段回调
		tokenStream.onPartialResponse(partialResponse -> sink.tryEmitNext(ServerSentEvent.builder(partialResponse).event("AiMessage").build()));
		//错误回调
		tokenStream.onError(sink::tryEmitError);
		//结束回调
		tokenStream.onCompleteResponse(aiMessageResponse -> sink.tryEmitComplete());
		tokenStream.start();
		return sink.asFlux();
	}

}
