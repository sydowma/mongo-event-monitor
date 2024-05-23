package com.sydow.mongoeventmonitor;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sydow.mongoeventmonitor.monitor.MongoMetricCommandListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    public MongoMetricCommandListener metricCommandListener() {
        return new MongoMetricCommandListener(meterRegistry);
    }
    
    @Bean
    public MongoDatabase mongoDatabase() {
        ConnectionString connectionString = new ConnectionString("mongodb://admin:adminpw@localhost:27017/");
        MongoClientSettings settings = MongoClientSettings.builder()
                .addCommandListener(metricCommandListener())
                .applyConnectionString(connectionString)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        return mongoClient.getDatabase("demo");
    }
    
}