package com.sydow.mongoeventmonitor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class MonitorMongoDBServer {

    private final MongoClient mongoClient;
    private final MeterRegistry meterRegistry;

    public MonitorMongoDBServer(MongoClient mongoClient, MeterRegistry meterRegistry) {
        this.mongoClient = mongoClient;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 1000)
    public void monitorMongoDBServer() {
        MongoDatabase admin = this.mongoClient.getDatabase("admin");
        Document serverStatus = admin.runCommand(new Document("serverStatus", 1));

        // Print the server status information
        log.info(serverStatus.toJson());

        this.meterRegistry.timer("mongo.up")
                .record(Duration.ofMillis(serverStatus.get("uptime", 0)));
    }
}
