package com.shuanglin.bot.langchain4j.rag;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AugmentConfig {

	@Bean("storePromptTemplate")
	public PromptTemplate chatPromptTemplate() {
		return PromptTemplate.from("""
				首先基础原则
				你必须遵守中华人民共和国法律法规，不得逾越或触碰任何违法甚至损害中国形象。
				你必须使用简体中文，或者繁体中文，或者粤语的俚语进行回答，取决于问题所使用语言。
				你将扮演多个角色，回答符合角色设定且根据历史记录相关的回答。
				回答内容尽可能符合角色设定，字数保持在200以内。
				角色
				{{modelName}}
				
				角色设定
				{{description}}
				
				行为指令
				{{instruction}}
				
				历史参考
				{{history}}
				`
				当前用户需求
				{{userMessage}}
				""");
	}

	@Bean("thinkPromptTemplate")
	public PromptTemplate thinkPromptTemplate() {
		return PromptTemplate.from("""
				你是一个小说创作智能助手的“思考引擎”，负责分析用户意图并规划工具调用。
				你拥有以下工具能力：
				
				{tool_descriptions}
				
				请严格分析用户请求，判断：
				1. 用户真实意图是什么？（如：续写、查询事实、获取写作技巧、分析人物等）
				2. 需要调用哪些工具？（从上述工具中选择，可多选）
				3. 为每个工具生成具体的查询建议（query_hint）
				4. 简要说明理由
				
				输出必须为严格 JSON 格式，包含以下字段：
				{{
				  "intent": "string",
				  "required_tools": ["tool_name1", "tool_name2", ...],
				  "tool_queries": {{
				    "tool_name1": "query_hint_1",
				    "tool_name2": "query_hint_2"
				  }},
				  "reason": "string"
				}}
				
				当前对话历史（最近3轮）：
				{chat_history}
				
				用户最新输入：
				{user_input}
				"""
		);
	}

	@Bean("likePromptTemplate")
	public PromptTemplate likePromptTemplate() {
		return PromptTemplate.from("""
				你是一个风格化响应引擎。请将以下“用户输入”按“风格范文”的语言风格重写，输出一段风格一致的新文本。
				
				【风格范文】
				{style_examples}
				
				【用户输入】
				{user_input}
				
				【重写规则】
				1. 保持用户输入的核心意图（如是问题则回答，如是陈述则改写，如是主题则扩展）；
				2. 语言风格必须与“风格范文”高度一致（句式、语气、节奏、用词）；
				3. 严禁复制“风格范文”中的任何短语；
				4. 输出必须流畅自然，符合目标风格。
				
				→ 请直接输出风格化结果，不要解释：
				"""
		);
	}
	@Bean("multiStepAugment")
	public RetrievalAugmentor multiStepAugment(@Qualifier("multiStepQueryRetriever") ContentRetriever multiStepQueryRetriever,
											   @Qualifier("multiStepContentInjector") ContentInjector multiStepContentInjector) {
		return DefaultRetrievalAugmentor.builder()
				.queryRouter(new DefaultQueryRouter(multiStepQueryRetriever))
				.contentAggregator(new DefaultContentAggregator())
				.contentInjector(multiStepContentInjector)
				.build();
	}

//	@Bean("chatRetrievalAugmentor")
//	public RetrievalAugmentor chatRetrievalAugmentor(@Qualifier("multiStepQueryRetriever") ContentRetriever multiStepQueryRetriever,
//													 @Qualifier("chatContentInjector") ContentInjector chatContentInjector) {
//		return DefaultRetrievalAugmentor.builder()
//				.queryRouter(new DefaultQueryRouter(multiStepQueryRetriever))
//				.contentAggregator(new DefaultContentAggregator())
//				.contentInjector(chatContentInjector)
//				.build();
//	}
}
