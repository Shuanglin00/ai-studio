package com.shuanglin.dao.Articles;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticlesEntityRepository extends MongoRepository<ArticlesEntity, String> {

}
