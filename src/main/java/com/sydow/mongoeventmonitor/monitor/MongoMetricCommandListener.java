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
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MongoMetricCommandListener implements CommandListener, MeterBinder {

    private final MeterRegistry meterRegistry;
    private final Counter commandSuccessCounter;
    private final Counter commandErrorCounter;
    private final Timer commandTimer;

    private static final String START_TIME = "MONGO-StartTime";

    private final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();

    public MongoMetricCommandListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.commandSuccessCounter = meterRegistry.counter("mongodb.command.success");
        this.commandErrorCounter = meterRegistry.counter("mongodb.command.error");
        this.commandTimer = meterRegistry.timer("mongodb.command.timer");
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        meterRegistry.gauge("mongodb.command.rate.oneMinute", commandSuccessCounter, counter -> counter.count() / 60);
        meterRegistry.gauge("mongodb.command.rate.fiveMinutes", commandSuccessCounter, counter -> counter.count() / 300);
        meterRegistry.gauge("mongodb.command.rate.fifteenMinutes", commandSuccessCounter, counter -> counter.count() / 900);
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        long startTime = startTimeHolder.get();
        long duration = System.currentTimeMillis() - startTime;
        this.collectMetric(event.getCommandName(), duration, false);
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        startTimeHolder.set(System.currentTimeMillis());
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        BsonDocument bsonDocument = event.getResponse();
        BsonValue writeErrors = bsonDocument.get("writeErrors");
        boolean success = writeErrors == null;

        long startTime = startTimeHolder.get();
        long duration = System.currentTimeMillis() - startTime;
        this.collectMetric(event.getCommandName(), duration, success);
    }

    private void collectMetric(String commandName, long duration, boolean success) {
        commandTimer.record(duration, TimeUnit.MILLISECONDS);

        if (success) {
            commandSuccessCounter.increment();
        } else {
            commandErrorCounter.increment();
        }
    }
}