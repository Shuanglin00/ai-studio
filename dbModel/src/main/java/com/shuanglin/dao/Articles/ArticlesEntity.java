package com.shuanglin.dao.Articles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Articles_store")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArticlesEntity {
	private String id;

	private String title;

	private String content;

	private String tags;

	private String createTime;

	// ========== 知识图谱构建扩展字段 ==========
	
	/** 书籍唯一标识（UUID生成），用于关联同一本书的所有章节 */
	private String bookUuid;
	
	/** 章节序号（1-N），用于排序和查询 */
	private Integer chapterIndex;
	
	/** LLM生成的原始Cypher语句 */
	private String cypherStatements;
	
	/** Cypher执行状态：SUCCESS/FAILED/PENDING */
	private String cypherExecuteStatus;
	
	/** Cypher执行时间（ISO时间字符串） */
	private String cypherExecuteTime;
	
	/** Cypher执行错误信息 */
	private String cypherErrorMessage;
	
	/** 章节处理状态：PENDING/PROCESSING/COMPLETED/FAILED */
	private String processStatus;
	
	/** 段落总数 */
	private Integer paragraphCount;
	
	/** 数据源标识（用于数据隔离） */
	private String dataSource;
	
	/** 扩展元数据（JSON字符串） */
	private String metadata;
}
