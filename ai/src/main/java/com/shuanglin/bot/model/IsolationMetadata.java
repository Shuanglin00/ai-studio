package com.shuanglin.bot.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据隔离元数据
 * 用于在Neo4j、MongoDB、Milvus等多个存储系统中实现数据隔离
 */
public class IsolationMetadata {
	
	/** 数据源标识（如：test_epub_40） */
	private String dataSource;
	
	/** 书籍名称 */
	private String bookName;
	
	/** 书籍唯一标识（UUID生成） */
	private String bookUuid;
	
	/** 章节数量限制 */
	private Integer chapterLimit;
	
	/** 创建者标识 */
	private String createdBy;
	
	/** 创建时间 */
	private String createdAt;
	
	/** 自定义标签 */
	private Map<String, String> tags;

	public IsolationMetadata() {
		this.createdAt = Instant.now().toString();
		this.tags = new HashMap<>();
	}

	public IsolationMetadata(String dataSource, String bookName, String bookUuid) {
		this();
		this.dataSource = dataSource;
		this.bookName = bookName;
		this.bookUuid = bookUuid;
	}

	/**
	 * 验证元数据的有效性
	 * @return 是否有效
	 * @throws IllegalArgumentException 如果验证失败
	 */
	public boolean validate() {
		if (dataSource == null || dataSource.trim().isEmpty()) {
			throw new IllegalArgumentException("dataSource不能为空");
		}
		if (!dataSource.matches("^[a-zA-Z0-9_]+$")) {
			throw new IllegalArgumentException("dataSource格式不合法，仅支持字母、数字、下划线");
		}
		if (bookName == null || bookName.trim().isEmpty()) {
			throw new IllegalArgumentException("bookName不能为空");
		}
		if (bookUuid == null || bookUuid.trim().isEmpty()) {
			throw new IllegalArgumentException("bookUuid不能为空");
		}
		if (chapterLimit != null && (chapterLimit < 1 || chapterLimit > 1000)) {
			throw new IllegalArgumentException("chapterLimit超出允许范围（1-1000）");
		}
		return true;
	}

	// Getters and Setters
	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getBookName() {
		return bookName;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	public String getBookUuid() {
		return bookUuid;
	}

	public void setBookUuid(String bookUuid) {
		this.bookUuid = bookUuid;
	}

	public Integer getChapterLimit() {
		return chapterLimit;
	}

	public void setChapterLimit(Integer chapterLimit) {
		this.chapterLimit = chapterLimit;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public void addTag(String key, String value) {
		if (this.tags == null) {
			this.tags = new HashMap<>();
		}
		this.tags.put(key, value);
	}

	@Override
	public String toString() {
		return "IsolationMetadata{" +
				"dataSource='" + dataSource + '\'' +
				", bookName='" + bookName + '\'' +
				", bookUuid='" + bookUuid + '\'' +
				", chapterLimit=" + chapterLimit +
				", createdBy='" + createdBy + '\'' +
				", createdAt='" + createdAt + '\'' +
				", tags=" + tags +
				'}';
	}
}
