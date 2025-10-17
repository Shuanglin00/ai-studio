package com.shuanglin.bot.model;

/**
 * 处理报告
 * 记录章节处理的统计信息
 */
public class ProcessReport {
	
	/** 书籍UUID */
	private String bookUuid;
	
	/** 书籍名称 */
	private String bookName;
	
	/** 处理章节总数 */
	private Integer totalChapters;
	
	/** 成功章节数 */
	private Integer successChapters;
	
	/** 失败章节数 */
	private Integer failedChapters;
	
	/** 跳过章节数 */
	private Integer skippedChapters;
	
	/** 处理总耗时（毫秒） */
	private Long totalDuration;
	
	/** 平均每章处理耗时（毫秒） */
	private Long avgChapterDuration;

	public ProcessReport() {
	}

	public ProcessReport(String bookUuid, String bookName) {
		this.bookUuid = bookUuid;
		this.bookName = bookName;
		this.totalChapters = 0;
		this.successChapters = 0;
		this.failedChapters = 0;
		this.skippedChapters = 0;
		this.totalDuration = 0L;
		this.avgChapterDuration = 0L;
	}

	// Getters and Setters
	public String getBookUuid() {
		return bookUuid;
	}

	public void setBookUuid(String bookUuid) {
		this.bookUuid = bookUuid;
	}

	public String getBookName() {
		return bookName;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	public Integer getTotalChapters() {
		return totalChapters;
	}

	public void setTotalChapters(Integer totalChapters) {
		this.totalChapters = totalChapters;
	}

	public Integer getSuccessChapters() {
		return successChapters;
	}

	public void setSuccessChapters(Integer successChapters) {
		this.successChapters = successChapters;
	}

	public Integer getFailedChapters() {
		return failedChapters;
	}

	public void setFailedChapters(Integer failedChapters) {
		this.failedChapters = failedChapters;
	}

	public Integer getSkippedChapters() {
		return skippedChapters;
	}

	public void setSkippedChapters(Integer skippedChapters) {
		this.skippedChapters = skippedChapters;
	}

	public Long getTotalDuration() {
		return totalDuration;
	}

	public void setTotalDuration(Long totalDuration) {
		this.totalDuration = totalDuration;
	}

	public Long getAvgChapterDuration() {
		return avgChapterDuration;
	}

	public void setAvgChapterDuration(Long avgChapterDuration) {
		this.avgChapterDuration = avgChapterDuration;
	}

	@Override
	public String toString() {
		return "\n📊 知识图谱构建报告\n" +
				"========================================\n" +
				"书籍信息: " + bookName + " (" + bookUuid + ")\n" +
				"处理章节: " + totalChapters + " 章\n" +
				"成功: " + successChapters + " | 失败: " + failedChapters + " | 跳过: " + skippedChapters + "\n" +
				"总耗时: " + (totalDuration / 1000.0) + " 秒\n" +
				"平均每章: " + (avgChapterDuration / 1000.0) + " 秒\n" +
				"========================================";
	}
}
