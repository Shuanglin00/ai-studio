package com.shuanglin.bot.langchain4j.config.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.milvus.plus.config.MilvusPropertiesConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = MilvusProperties.PREFIX)
public class MilvusProperties extends MilvusPropertiesConfiguration {
	public static final String PREFIX = "milvus";

	private String uri;

	private String host;

	private Integer port;

	private String username;

	private String password;

	private String dbName;

	private Integer topK;

	/**
	 * 向量集合名（RAG向量表名）
	 */
	private String messageCollectionName;

}
