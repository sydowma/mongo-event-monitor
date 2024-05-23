package com.sydow.mongoeventmonitor;

import com.mongodb.client.result.InsertOneResult;

import java.util.List;

public interface UserDao {
    
    UserEntity findById(long id);
    
    List<UserEntity> findAll();

    InsertOneResult save(UserEntity userEntity);

    void delete(long id);
    
}