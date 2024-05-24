package com.sydow.mongoeventmonitor;

import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Component
public class UpdateRandomly {

    @Autowired
    private UserDao userDao;

    private Random random = SecureRandom.getInstanceStrong();

    public UpdateRandomly() throws NoSuchAlgorithmException {
    }

    /**
     *
     */
    @Scheduled(cron = "0/1 * * * * *")
    public void updateRandomly() {
        List<UserEntity> userEntities = new ArrayList<>(1000);
        for(int i = 0; i < 1000; i++) {
            UserEntity userEntity = new UserEntity();
            long now = System.nanoTime() + random.nextInt(1000000);
            userEntity.setId(now);
            userEntity.setName("name-" + now);
            userEntity.setCreateTime(new Date(now));
            userEntity.setModifyTime(new Date(now));
            userEntities.add(userEntity);
        }


        InsertManyResult save = userDao.save(userEntities);

        log.info("save userEntity: {}", save);
        log.info("delete userEntity: {}", save);

        performSlowQuery();

        List<Long> list = userEntities.stream().map(UserEntity::getId).toList();

        userDao.delete(list);
    }

    /**
     * This method performs a slow query to demonstrate the monitoring capabilities.
    */
    private void performSlowQuery() {
        log.info("Starting slow query...");

        List<UserEntity> result = userDao.aggregate();

        log.info("Slow query result size: {}", result.size());
    }
}
