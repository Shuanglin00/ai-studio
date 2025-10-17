package com.shuanglin.bot.model;

import java.time.Instant;

/**
 * 数据清理报告
 */
public class CleanupReport {
	
	/** 数据源标识 */
	private String dataSource;
	
	/** Neo4j删除节点数 */
	private Integer neo4jNodesDeleted;
	
	/** Neo4j删除关系数 */
	private Integer neo4jRelationsDeleted;
	
	/** MongoDB删除文档数 */
	private Long mongoDocsDeleted;
	
	/** 清理耗时（毫秒） */
	private Long cleanupDuration;
	
	/** 清理时间 */
	private String cleanupTime;

	public CleanupReport() {
		this.cleanupTime = Instant.now().toString();
	}

	public CleanupReport(String dataSource) {
		this();
		this.dataSource = dataSource;
		this.neo4jNodesDeleted = 0;
		this.neo4jRelationsDeleted = 0;
		this.mongoDocsDeleted = 0L;
		this.cleanupDuration = 0L;
	}

	// Getters and Setters
	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public Integer getNeo4jNodesDeleted() {
		return neo4jNodesDeleted;
	}

	public void setNeo4jNodesDeleted(Integer neo4jNodesDeleted) {
		this.neo4jNodesDeleted = neo4jNodesDeleted;
	}

	public Integer getNeo4jRelationsDeleted() {
		return neo4jRelationsDeleted;
	}

	public void setNeo4jRelationsDeleted(Integer neo4jRelationsDeleted) {
		this.neo4jRelationsDeleted = neo4jRelationsDeleted;
	}

	public Long getMongoDocsDeleted() {
		return mongoDocsDeleted;
	}

	public void setMongoDocsDeleted(Long mongoDocsDeleted) {
		this.mongoDocsDeleted = mongoDocsDeleted;
	}

	public Long getCleanupDuration() {
		return cleanupDuration;
	}

	public void setCleanupDuration(Long cleanupDuration) {
		this.cleanupDuration = cleanupDuration;
	}

	public String getCleanupTime() {
		return cleanupTime;
	}

	public void setCleanupTime(String cleanupTime) {
		this.cleanupTime = cleanupTime;
	}

	@Override
	public String toString() {
		return "\n🧹 数据清理报告\n" +
				"========================================\n" +
				"数据源: " + dataSource + "\n" +
				"Neo4j删除节点: " + neo4jNodesDeleted + "\n" +
				"Neo4j删除关系: " + neo4jRelationsDeleted + "\n" +
				"MongoDB删除文档: " + mongoDocsDeleted + "\n" +
				"清理耗时: " + (cleanupDuration / 1000.0) + " 秒\n" +
				"清理时间: " + cleanupTime + "\n" +
				"========================================";
	}
}
