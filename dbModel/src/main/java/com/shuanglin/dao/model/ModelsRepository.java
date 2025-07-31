package com.shuanglin.dao.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelsRepository extends MongoRepository<Model, String> {

	List<Model> getModelsByIsActive(String isActive);


	Model getModelByModelName(String modelName);

}
