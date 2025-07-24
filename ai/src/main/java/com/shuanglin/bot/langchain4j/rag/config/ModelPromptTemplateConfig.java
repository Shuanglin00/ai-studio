package com.shuanglin.bot.langchain4j.rag.config;

import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelPromptTemplateConfig {
	@Bean
	public PromptTemplate promptTemplate() {
		return PromptTemplate.from(
				"""
						# 角色 (Role)
						你现在是一个 {{modelName}}。
						
						# 基础设定 (base design)
						{{knowledge}}
						
						# 任务指令 (Instruction)
						{{Instruction}}
						
						# 限制与要求 (Constraints & Requirements)
						-   使用简体中文回答；
						-   必须符合中华人民共和国法律法规，绝对不可以违反法律，遵守道德；
						-   返回内容限制200字以内；
						
						# 用户输入 (User Input)
						{{question}}
						
						---
				"""
		);
	}

}
