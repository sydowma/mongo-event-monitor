package com.sydow.mongoeventmonitor.monitor;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MongoMetricCommandListener implements CommandListener, MeterBinder {
   private final MeterRegistry meterRegistry;
    private final Counter commandSuccessCounter;
    private final Counter commandErrorCounter;
    private final Timer commandTimer;

    private static final String START_TIME = "MONGO-StartTime";

    private final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();
    private final ThreadLocal<String> collectionNameHolder = new ThreadLocal<>();

    public MongoMetricCommandListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.commandSuccessCounter = meterRegistry.counter("mongodb.command.success");
        this.commandErrorCounter = meterRegistry.counter("mongodb.command.error");
        this.commandTimer = meterRegistry.timer("mongodb.command.timer");
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        meterRegistry.gauge("mongodb.command.rate.oneMinute", commandSuccessCounter, Counter::count);
        meterRegistry.gauge("mongodb.command.rate.fiveMinutes", commandSuccessCounter, counter -> counter.count() / 5);
        meterRegistry.gauge("mongodb.command.rate.fifteenMinutes", commandSuccessCounter, counter -> counter.count() / 15);
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        long startTime = startTimeHolder.get();
        long duration = System.currentTimeMillis() - startTime;
        this.collectMetric(event.getCommandName(), event.getDatabaseName(), duration, false);
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        startTimeHolder.set(System.currentTimeMillis());
        String collectionName = event.getCommand().get(event.getCommandName()).asString().getValue();
        collectionNameHolder.set(collectionName);
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        long startTime = startTimeHolder.get();
        long duration = System.currentTimeMillis() - startTime;

        BsonDocument bsonDocument = event.getResponse();
        BsonValue writeErrors = bsonDocument.get("writeErrors");
        boolean success = writeErrors == null;

        this.collectMetric(event.getCommandName(), event.getDatabaseName(), duration, success);

        // 处理慢查询
        if (duration > 10) { // 假设超过1000ms为慢查询，你可以根据需要调整
            Timer slowQueryTimer = Timer.builder("mongodb.command.slowQuery.timer")
                    .tag("commandName", event.getCommandName())
                    .tag("databaseName", event.getDatabaseName())
                    .tag("collectionName", collectionNameHolder.get())
                    .register(meterRegistry);
            slowQueryTimer.record(duration, TimeUnit.MILLISECONDS);
            Counter slowQueryCounter = Counter.builder("mongodb.command.slowQuery.count")
                    .tag("commandName", event.getCommandName())
                    .tag("databaseName", event.getDatabaseName())
            .tag("collectionName", collectionNameHolder.get())
                    .register(meterRegistry);
            slowQueryCounter.increment();
        }
    }

    private void collectMetric(String commandName, String databaseName, long duration, boolean success) {
        Timer commandSpecificTimer = Timer.builder("mongodb.command.timer")
                .tag("commandName", commandName)
                .tag("databaseName", databaseName)
                .tag("collectionName", collectionNameHolder.get())
                .register(meterRegistry);
        log.info("Registered commandSpecificTimer: {}", commandSpecificTimer);
        commandSpecificTimer.record(duration, TimeUnit.MILLISECONDS);

        if (success) {
            commandSuccessCounter.increment();
        } else {
            commandErrorCounter.increment();
        }
    }
}