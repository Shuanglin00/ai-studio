package com.shuanglin.bot.service;

import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

public class LikeService {
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
	public String chat(){
		Map<String,String> map = new HashMap<>();
		likePromptTemplate().apply(map).toUserMessage();
		return "<UNK>";
	}
}
