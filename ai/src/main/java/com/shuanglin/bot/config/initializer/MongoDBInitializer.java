package com.shuanglin.bot.config.initializer;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * MongoDB 初始化器
 * 在应用启动时运行，用于检查并创建必要的集合 (Collections) 和索引 (Indexes)。
 * 这确保了应用所需的基础数据结构是存在的。
 */
@Component
@Slf4j
public class MongoDBInitializer implements ApplicationRunner {

	@Resource
	private MongoTemplate mongoTemplate;

	// 定义集合名称，方便管理
	private static final String MODELS_COLLECTION = "model";
	private static final String PERMISSIONS_COLLECTION = "groupModelPermissions";
	private static final String SETTINGS_COLLECTION = "userGroupModelSettings";
	// --- 新增：为 KnowledgeEntity 定义集合名称 ---
	private static final String KNOWLEDGE_COLLECTION = "message_store";

	@Override
	public void run(ApplicationArguments args) {
		log.info("--- 开始进行 MongoDB 集合初始化检查 ---");
		try {
			// 初始化 'models' 集合
			initializeModelsCollection();

			// 初始化 'groupModelPermissions' 集合
			initializeGroupModelPermissionsCollection();

			// 初始化 'userGroupModelSettings' 集合
			initializeUserGroupModelSettingsCollection();

			// --- 新增：初始化 'message_store' 集合 ---
			initializeKnowledgeCollection();

		} catch (Exception e) {
			log.error("MongoDB 初始化失败！应用可能无法正常工作。", e);
		} finally {
			log.info("--- MongoDB 集合初始化检查完成 ---");
		}
	}

	/**
	 * --- 新增方法 ---
	 * 初始化 `model_knowledge` 集合及其索引。
	 * - 复合索引: (groupId, userId, modelId)
	 * - 普通索引: lastChatTime
	 */
	private void initializeKnowledgeCollection() {
		if (!mongoTemplate.collectionExists(KNOWLEDGE_COLLECTION)) {
			mongoTemplate.createCollection(KNOWLEDGE_COLLECTION);
			log.info("集合 '{}' 不存在，已成功创建。", KNOWLEDGE_COLLECTION);
		}

		log.info("为集合 '{}' 检查并创建索引...", KNOWLEDGE_COLLECTION);
		MongoCollection<Document> collection = mongoTemplate.getCollection(KNOWLEDGE_COLLECTION);

		// 为 lastChatTime 创建倒序索引，以便快速按时间排序，获取最新记录
		// -1 表示倒序 (descending)
		collection.createIndex(new Document("lastChatTime", -1));
		log.info(" -> 倒序索引 on 'lastChatTime' 已确保存在。");

		// （可选）如果经常按类型过滤，也可以为 'type' 字段创建索引
		// collection.createIndex(new Document("type", 1));
		// log.info(" -> 普通索引 on 'type' 已确保存在。");
	}


	/**
	 * 初始化 `models` 集合及其索引。
	 * - 唯一索引: modelName
	 */
	private void initializeModelsCollection() {
		if (!mongoTemplate.collectionExists(MODELS_COLLECTION)) {
			mongoTemplate.createCollection(MODELS_COLLECTION);
			log.info("集合 '{}' 不存在，已成功创建。", MODELS_COLLECTION);
		}

		log.info("为集合 '{}' 检查并创建索引...", MODELS_COLLECTION);
		MongoCollection<Document> collection = mongoTemplate.getCollection(MODELS_COLLECTION);

		// 创建唯一索引，确保 modelName 不重复 (相当于 SQL 的 UNIQUE KEY)
		IndexOptions uniqueOptions = new IndexOptions().unique(true);
		collection.createIndex(new Document("modelName", 1), uniqueOptions);
		log.info(" -> 唯一索引 on 'modelName' 已确保存在。");
	}

	/**
	 * 初始化 `groupModelPermissions` 集合及其索引。
	 * - 复合唯一索引: (groupId, modelId)
	 * - 普通索引: groupId
	 */
	private void initializeGroupModelPermissionsCollection() {
		if (!mongoTemplate.collectionExists(PERMISSIONS_COLLECTION)) {
			mongoTemplate.createCollection(PERMISSIONS_COLLECTION);
			log.info("集合 '{}' 不存在，已成功创建。", PERMISSIONS_COLLECTION);
		}

		log.info("为集合 '{}' 检查并创建索引...", PERMISSIONS_COLLECTION);
		MongoCollection<Document> collection = mongoTemplate.getCollection(PERMISSIONS_COLLECTION);

		// 创建复合唯一索引，确保一个群对一个模型的授权是唯一的 (相当于 SQL 的 UNIQUE KEY)
		IndexOptions uniqueOptions = new IndexOptions().unique(true);
		Document compoundIndexKeys = new Document("groupId", 1).append("modelId", 1);
		collection.createIndex(compoundIndexKeys, uniqueOptions);
		log.info(" -> 复合唯一索引 on 'groupId' & 'modelId' 已确保存在。");

		// 为 groupId 创建索引，以便快速查询一个群拥有的所有模型权限 (相当于 SQL 的 INDEX)
		collection.createIndex(new Document("groupId", 1));
		log.info(" -> 普通索引 on 'groupId' 已确保存在。");
	}

	/**
	 * 初始化 `userGroupModelSettings` 集合及其索引。
	 * - 复合唯一索引: (userId, groupId)
	 */
	private void initializeUserGroupModelSettingsCollection() {
		if (!mongoTemplate.collectionExists(SETTINGS_COLLECTION)) {
			mongoTemplate.createCollection(SETTINGS_COLLECTION);
			log.info("集合 '{}' 不存在，已成功创建。", SETTINGS_COLLECTION);
		}

		log.info("为集合 '{}' 检查并创建索引...", SETTINGS_COLLECTION);
		MongoCollection<Document> collection = mongoTemplate.getCollection(SETTINGS_COLLECTION);

		// 创建复合唯一索引，保证一个用户在一个群里只能有一条设置记录 (相当于 SQL 的 PRIMARY KEY 或 UNIQUE KEY)
		IndexOptions uniqueOptions = new IndexOptions().unique(true);
		Document compoundIndexKeys = new Document("userId", 1).append("groupId", 1);
		collection.createIndex(compoundIndexKeys, uniqueOptions);
		log.info(" -> 复合唯一索引 on 'userId' & 'groupId' 已确保存在。");
	}
}