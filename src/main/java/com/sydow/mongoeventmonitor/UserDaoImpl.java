package com.sydow.mongoeventmonitor;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;


public class UserDaoImpl implements UserDao {
    
    
    private final MongoCollection<Document> collection;

    public UserDaoImpl(MongoCollection<Document> collection) {
        this.collection = collection;
    }


    @Override
    public UserEntity findById(long id) {
        Document document = new Document("_id", id);
        FindIterable<Document> documents = this.collection.find(document);
        while (documents.cursor().hasNext()) {
            Document currentDocument = documents.cursor().next();
            return this.convertToEntity(currentDocument);
        }
        return null;
    }

    private UserEntity convertToEntity(Document currentDocument) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(currentDocument.getLong("_id"));
        userEntity.setCreateTime(currentDocument.getDate("createTime"));
        userEntity.setModifyTime(currentDocument.getDate("modifyTime"));
        return userEntity;
    }

    @Override
    public List<UserEntity> findAll() {
        
        FindIterable<Document> documents = this.collection.find();
        List<UserEntity> result = new ArrayList<>();
        while (documents.cursor().hasNext()) {
            Document currentDocument = documents.cursor().next();
            result.add(this.convertToEntity(currentDocument));
        }
        return result;
    }
}