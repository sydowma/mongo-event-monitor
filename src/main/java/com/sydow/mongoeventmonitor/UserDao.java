package com.sydow.mongoeventmonitor;

import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;

import java.util.List;

public interface UserDao {
    
    UserEntity findById(long id);
    
    List<UserEntity> findAll();

    InsertOneResult save(UserEntity userEntity);

    InsertManyResult save(List<UserEntity> userEntity);

    void delete(long id);
    
    void delete(List<Long> id);

    List<UserEntity> aggregate();
}