package com.shuanglin.dbModel.models;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelsRepository extends MongoRepository<Model, String> {

}
