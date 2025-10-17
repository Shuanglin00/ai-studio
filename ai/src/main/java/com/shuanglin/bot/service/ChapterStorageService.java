package com.shuanglin.bot.service;

import com.shuanglin.dao.Articles.ArticlesEntity;
import com.shuanglin.dao.Articles.ArticlesEntityRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 章节存储服务
 * 封装ArticlesEntity的MongoDB操作，提供业务级别的章节存储管理
 */
@Service
public class ChapterStorageService {

	@Resource
	private ArticlesEntityRepository articlesRepository;

	/**
	 * 保存章节内容和Cypher语句
	 * @param entity 章节实体
	 * @return 保存后的实体
	 */
	public ArticlesEntity saveChapterWithCypher(ArticlesEntity entity) {
		return articlesRepository.save(entity);
	}

	/**
	 * 更新章节的Cypher语句内容
	 * @param entity 章节实体
	 * @return 更新后的实体
	 */
	public ArticlesEntity updateCypherContent(ArticlesEntity entity) {
		return articlesRepository.save(entity);
	}

	/**
	 * 更新Cypher执行状态
	 * @param docId 文档ID
	 * @param status 执行状态（SUCCESS/FAILED/PENDING）
	 * @param errorMsg 错误信息（可选）
	 * @param executeTime 执行时间
	 * @return 是否更新成功
	 */
	public boolean updateCypherExecuteStatus(String docId, String status, String errorMsg, String executeTime) {
		Optional<ArticlesEntity> optionalEntity = articlesRepository.findById(docId);
		if (optionalEntity.isPresent()) {
			ArticlesEntity entity = optionalEntity.get();
			entity.setCypherExecuteStatus(status);
			entity.setCypherExecuteTime(executeTime);
			if (errorMsg != null) {
				entity.setCypherErrorMessage(errorMsg);
			}
			// 根据执行状态更新处理状态
			if ("SUCCESS".equals(status)) {
				entity.setProcessStatus("COMPLETED");
			} else if ("FAILED".equals(status)) {
				entity.setProcessStatus("FAILED");
			}
			articlesRepository.save(entity);
			return true;
		}
		return false;
	}

	/**
	 * 批量保存章节数据
	 * @param entities 章节实体列表
	 * @return 保存后的实体列表
	 */
	public List<ArticlesEntity> batchSaveChapters(List<ArticlesEntity> entities) {
		return articlesRepository.saveAll(entities);
	}

	/**
	 * 查询书籍所有章节（按索引排序）
	 * @param bookUuid 书籍唯一标识
	 * @return 章节列表
	 */
	public List<ArticlesEntity> queryChaptersByBook(String bookUuid) {
		return articlesRepository.findByBookUuidOrderByChapterIndexAsc(bookUuid);
	}

	/**
	 * 获取指定章节
	 * @param bookUuid 书籍唯一标识
	 * @param chapterIndex 章节索引
	 * @return 章节实体
	 */
	public ArticlesEntity queryChapterByIndex(String bookUuid, Integer chapterIndex) {
		return articlesRepository.findByBookUuidAndChapterIndex(bookUuid, chapterIndex).orElse(null);
	}

	/**
	 * 获取指定章节的Cypher语句
	 * @param bookUuid 书籍唯一标识
	 * @param chapterIndex 章节索引
	 * @return Cypher语句
	 */
	public String queryCypherByChapter(String bookUuid, Integer chapterIndex) {
		ArticlesEntity entity = queryChapterByIndex(bookUuid, chapterIndex);
		return entity != null ? entity.getCypherStatements() : null;
	}

	/**
	 * 查询处理失败的章节
	 * @param bookUuid 书籍唯一标识
	 * @return 失败章节列表
	 */
	public List<ArticlesEntity> queryFailedChapters(String bookUuid) {
		return articlesRepository.findByBookUuidAndCypherExecuteStatus(bookUuid, "FAILED");
	}

	/**
	 * 删除书籍所有数据
	 * @param bookUuid 书籍唯一标识
	 * @return 删除的文档数
	 */
	public Long deleteBookData(String bookUuid) {
		return articlesRepository.deleteByBookUuid(bookUuid);
	}

	/**
	 * 获取书籍处理统计信息
	 * @param bookUuid 书籍唯一标识
	 * @return 统计信息
	 */
	public BookStats getBookStatistics(String bookUuid) {
		List<ArticlesEntity> allChapters = articlesRepository.findByBookUuid(bookUuid);
		
		BookStats stats = new BookStats();
		stats.setBookUuid(bookUuid);
		stats.setTotalChapters(allChapters.size());
		
		long completedCount = allChapters.stream()
				.filter(e -> "COMPLETED".equals(e.getProcessStatus()))
				.count();
		stats.setCompletedChapters((int) completedCount);
		
		long failedCount = allChapters.stream()
				.filter(e -> "FAILED".equals(e.getProcessStatus()))
				.count();
		stats.setFailedChapters((int) failedCount);
		
		long pendingCount = allChapters.stream()
				.filter(e -> "PENDING".equals(e.getProcessStatus()))
				.count();
		stats.setPendingChapters((int) pendingCount);
		
		long successCypherCount = allChapters.stream()
				.filter(e -> "SUCCESS".equals(e.getCypherExecuteStatus()))
				.count();
		stats.setSuccessCypherCount((int) successCypherCount);
		
		long failedCypherCount = allChapters.stream()
				.filter(e -> "FAILED".equals(e.getCypherExecuteStatus()))
				.count();
		stats.setFailedCypherCount((int) failedCypherCount);
		
		return stats;
	}

	/**
	 * 书籍统计信息
	 */
	public static class BookStats {
		private String bookUuid;
		private Integer totalChapters = 0;
		private Integer completedChapters = 0;
		private Integer failedChapters = 0;
		private Integer pendingChapters = 0;
		private Integer successCypherCount = 0;
		private Integer failedCypherCount = 0;

		// Getters and Setters
		public String getBookUuid() {
			return bookUuid;
		}

		public void setBookUuid(String bookUuid) {
			this.bookUuid = bookUuid;
		}

		public Integer getTotalChapters() {
			return totalChapters;
		}

		public void setTotalChapters(Integer totalChapters) {
			this.totalChapters = totalChapters;
		}

		public Integer getCompletedChapters() {
			return completedChapters;
		}

		public void setCompletedChapters(Integer completedChapters) {
			this.completedChapters = completedChapters;
		}

		public Integer getFailedChapters() {
			return failedChapters;
		}

		public void setFailedChapters(Integer failedChapters) {
			this.failedChapters = failedChapters;
		}

		public Integer getPendingChapters() {
			return pendingChapters;
		}

		public void setPendingChapters(Integer pendingChapters) {
			this.pendingChapters = pendingChapters;
		}

		public Integer getSuccessCypherCount() {
			return successCypherCount;
		}

		public void setSuccessCypherCount(Integer successCypherCount) {
			this.successCypherCount = successCypherCount;
		}

		public Integer getFailedCypherCount() {
			return failedCypherCount;
		}

		public void setFailedCypherCount(Integer failedCypherCount) {
			this.failedCypherCount = failedCypherCount;
		}

		@Override
		public String toString() {
			return "BookStats{" +
					"bookUuid='" + bookUuid + '\'' +
					", totalChapters=" + totalChapters +
					", completedChapters=" + completedChapters +
					", failedChapters=" + failedChapters +
					", pendingChapters=" + pendingChapters +
					", successCypherCount=" + successCypherCount +
					", failedCypherCount=" + failedCypherCount +
					'}';
		}
	}
}
