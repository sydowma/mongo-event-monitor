package com.sydow.mongoeventmonitor;

import java.util.List;

public interface UserDao {
    
    UserEntity findById(long id);
    
    List<UserEntity> findAll();
    
}