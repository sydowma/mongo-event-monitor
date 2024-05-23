package com.sydow.mongoeventmonitor;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaoConfig {

    @Autowired
    private MongoDatabase mongoDatabase;


    @Bean
    public UserDao userDao() {
        MongoCollection<Document> user = this.mongoDatabase.getCollection("user");
        return new UserDaoImpl(user);
    }

}
