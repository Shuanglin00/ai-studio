package com.shuanglin.bot.db;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface MessageStoreEntityRepository extends MongoRepository<MessageStoreEntity, String> {
	List<MessageStoreEntity> findByGroupIdAndUserIdAndModelId(String groupId, String userId, String modelId);

}
