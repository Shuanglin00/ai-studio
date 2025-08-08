package com.shuanglin.bot.langchain4j.config.vo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = MilvusProperties.PREFIX)
public class MilvusProperties {
	public static final String PREFIX = "milvus";

	private String url;

	private String host;

	private Integer port;

	private String username;

	private String password;

	private String defaultDatabaseName;

	private String messageCollectionName;

	private Integer topK;

}
