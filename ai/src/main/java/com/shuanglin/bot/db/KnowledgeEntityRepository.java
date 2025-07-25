package com.shuanglin.bot.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEntityRepository extends MongoRepository<KnowledgeEntity, String> {
	List<KnowledgeEntity> findKnowledge(String groupId, String userId, String modelId);
}
