package com.sydow.mongoeventmonitor;

import com.mongodb.client.result.InsertOneResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class UpdateRandomly {

    @Autowired
    private UserDao userDao;

    /**
     *
     */
    @Scheduled(cron = "0/1 * * * * *")
    public void updateRandomly() {
        UserEntity userEntity = new UserEntity();
        long now = System.currentTimeMillis();
        userEntity.setId(now);
        userEntity.setName("name-" + now);
        userEntity.setCreateTime(new Date(now));
        userEntity.setModifyTime(new Date(now));

        InsertOneResult save = userDao.save(userEntity);

        log.info("save userEntity: {}", save);
        userDao.delete(now);
        log.info("delete userEntity: {}", save);
    }
}
