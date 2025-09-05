package com.shuanglin.dao.message;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface MessageStoreEntityRepository extends MongoRepository<MessageStoreEntity, String> {

}
