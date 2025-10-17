package com.shuanglin.dao.Articles;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticlesEntityRepository extends MongoRepository<ArticlesEntity, String> {

	/**
	 * 查询指定书籍的所有章节
	 * @param bookUuid 书籍唯一标识
	 * @return 章节列表
	 */
	List<ArticlesEntity> findByBookUuid(String bookUuid);

	/**
	 * 查询指定章节
	 * @param bookUuid 书籍唯一标识
	 * @param chapterIndex 章节索引
	 * @return 章节实体
	 */
	Optional<ArticlesEntity> findByBookUuidAndChapterIndex(String bookUuid, Integer chapterIndex);

	/**
	 * 查询指定数据源的所有章节
	 * @param dataSource 数据源标识
	 * @return 章节列表
	 */
	List<ArticlesEntity> findByDataSource(String dataSource);

	/**
	 * 查询书籍章节并按索引排序
	 * @param bookUuid 书籍唯一标识
	 * @return 排序后的章节列表
	 */
	List<ArticlesEntity> findByBookUuidOrderByChapterIndexAsc(String bookUuid);

	/**
	 * 查询指定执行状态的章节
	 * @param status Cypher执行状态
	 * @return 章节列表
	 */
	List<ArticlesEntity> findByCypherExecuteStatus(String status);

	/**
	 * 查询指定书籍和执行状态的章节
	 * @param bookUuid 书籍唯一标识
	 * @param status Cypher执行状态
	 * @return 章节列表
	 */
	List<ArticlesEntity> findByBookUuidAndCypherExecuteStatus(String bookUuid, String status);

	/**
	 * 删除指定书籍的所有章节
	 * @param bookUuid 书籍唯一标识
	 * @return 删除的文档数
	 */
	Long deleteByBookUuid(String bookUuid);

	/**
	 * 删除指定数据源的所有章节
	 * @param dataSource 数据源标识
	 * @return 删除的文档数
	 */
	Long deleteByDataSource(String dataSource);

	/**
	 * 统计指定状态的章节数
	 * @param bookUuid 书籍唯一标识
	 * @param status Cypher执行状态
	 * @return 章节数量
	 */
	Long countByBookUuidAndCypherExecuteStatus(String bookUuid, String status);
}
