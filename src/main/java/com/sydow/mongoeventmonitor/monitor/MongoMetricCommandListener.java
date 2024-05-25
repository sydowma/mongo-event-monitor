package com.sydow.mongoeventmonitor.monitor;

import com.mongodb.connection.ConnectionDescription;
import com.mongodb.connection.ConnectionId;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MongoMetricCommandListener implements CommandListener {
    static final Set<String> COMMANDS_WITH_COLLECTION_NAME = new LinkedHashSet<>(Arrays.asList(
            "aggregate", "count", "distinct", "mapReduce", "geoSearch", "delete", "find", "findAndModify",
            "insert", "update", "collMod", "compact", "convertToCapped", "create", "createIndexes", "drop",
            "dropIndexes", "killCursors", "listIndexes", "reIndex"));

    private final DefaultMeter defaultMeter; // Assuming DefaultMeter is your custom class providing rates

    private static final int SLOW_QUERY_TIME = 300;
    public static final String ADMIN = "admin";
    private final MeterRegistry meterRegistry;

    private final Counter commandSuccessCounter;
    private final Counter commandErrorCounter;
    private final Map<String, Timer> timerCache = new HashMap<>();
    private final Map<String, Counter> errorCounterCache = new HashMap<>();
    private final Map<String, Counter> slowQueryCounterCache = new HashMap<>();
    private final Map<String, Counter> commandFailedCounterCache = new HashMap<>();

    private final AtomicLong oneMinuteCounter = new AtomicLong();
    private final AtomicLong fiveMinuteCounter = new AtomicLong();
    private final AtomicLong fifteenMinuteCounter = new AtomicLong();

    public MongoMetricCommandListener(MeterRegistry meterRegistry) {
        Meter.Id meterId = new Meter.Id("mongodb.default.meter",
                                        Tags.of(List.of(Tag.of("type", "custom"))), null, null, Meter.Type.GAUGE);
        defaultMeter = new DefaultMeter(meterId, 1.0, 5.0, 15.0);
        this.meterRegistry = meterRegistry;
        this.commandSuccessCounter = meterRegistry.counter("mongodb.command.success");
        this.commandErrorCounter = meterRegistry.counter("mongodb.command.error");
        this.registerGauges();
    }

    private Timer getOrCreateTimer(String commandName, String databaseName, String clusterId) {
        String key = commandName + ":" + databaseName + ":" + clusterId;
        return timerCache.computeIfAbsent(key, k -> Timer.builder("mongodb.command.timer")
                    .tag("commandName", commandName)
                    .tag("databaseName", databaseName)
                    .tag("mongodb.cluster_id", clusterId)
                    .register(meterRegistry));
    }

    private Counter getOrCreateErrorCounter(String commandName, String databaseName, String clusterId, Map<String, String> msg) {
        String key = commandName + ":" + databaseName + ":" + clusterId + ":" + msg;
        Counter.Builder tag = Counter.builder("mongodb.command.error.count")
            .tag("commandName", commandName)
            .tag("databaseName", databaseName)
            .tag("mongodb.cluster_id", clusterId);
        for (Map.Entry<String, String> entry : msg.entrySet()) {
            tag.tag("code", entry.getValue());
            tag.tag("key", entry.getKey());
        }
        Counter register = tag.register(meterRegistry);
        return errorCounterCache.computeIfAbsent(key, k -> register);
    }

    private Counter getOrCreateSlowQueryCounter(String commandName, String databaseName, String clusterId) {
        String key = commandName + ":" + databaseName + ":" + clusterId;
        return slowQueryCounterCache.computeIfAbsent(key, k -> Counter.builder("mongodb.command.slowQuery.count")
                    .tag("commandName", commandName)
                    .tag("databaseName", databaseName)
                    .tag("mongodb.cluster_id", clusterId)
                    .register(meterRegistry));
    }

    private Counter getOrCreateCommandFailedCounter(String commandName, String databaseName, String clusterId, String msg) {
        String key = commandName + ":" + databaseName + ":" + clusterId + ":" + msg;
        return commandFailedCounterCache.computeIfAbsent(key, k -> Counter.builder("mongodb.command.failed.count")
                    .tag("commandName", commandName)
                    .tag("databaseName", databaseName)
                    .tag("mongodb.cluster_id", clusterId)
                    .tag("msg", msg)
                    .register(meterRegistry));
    }

    @Override
        public void commandStarted(CommandStartedEvent commandStartedEvent) {
        // Implementation not required for this example
    }

    @Override
        public void commandSucceeded(CommandSucceededEvent event) {
        long duration = event.getElapsedTime(TimeUnit.MILLISECONDS);

        BsonDocument bsonDocument = event.getResponse();
        BsonValue writeErrors = bsonDocument.get("writeErrors");
        boolean success = writeErrors == null;

        if (!COMMANDS_WITH_COLLECTION_NAME.contains(event.getCommandName())) {
            return;
        }

        if (ADMIN.equals(event.getDatabaseName())) {
            return;
        }

        Map<String, String> msg = findMsg(success, writeErrors);

        this.collectMetric(event.getCommandName(), event.getDatabaseName(), duration, success, event.getConnectionDescription(), msg);
    }

    private static Map<String, String> findMsg(boolean success, BsonValue writeErrors) {
        Map<String, String> msg = new HashMap<>();
        if (!success) {
            if (writeErrors.isArray()) {
                for (BsonValue bsonValue : writeErrors.asArray()) {
                    if (bsonValue.isDocument()) {
                        BsonDocument document = bsonValue.asDocument();
                        BsonValue errorCode = document.get("code");
                        BsonValue keyPattern = document.get("keyPattern");
                        int code = errorCode.asInt32().getValue();
                        String key = keyPattern.toString();
                        msg.put(key, String.valueOf(code));
                        break;
                    }
                }
            }
        }
        return msg;
    }

    private void collectMetric(String commandName, String databaseName, long duration, boolean success,
                               ConnectionDescription connectionDescription, Map<String, String> msg) {
        String clusterId = connectionDescription.getConnectionId().getServerId().getClusterId().getValue();
        Timer commandSpecificTimer = this.getOrCreateTimer(commandName, databaseName, clusterId);
        commandSpecificTimer.record(duration, TimeUnit.MILLISECONDS);

        if (success) {
            commandSuccessCounter.increment();
        } else {
            commandErrorCounter.increment();
        }

        if (!msg.isEmpty()) {
            Counter errorCounter = getOrCreateErrorCounter(commandName, databaseName, clusterId, msg);
            errorCounter.increment();
        }

        // slow query
        if (duration > SLOW_QUERY_TIME) {
            Timer slowQueryTimer = Timer.builder("mongodb.command.slowQuery.timer")
                        .tag("commandName", commandName)
                        .tag("databaseName", databaseName)
                        .tag("mongodb.cluster_id", clusterId)
                        .register(meterRegistry);
            slowQueryTimer.record(duration, TimeUnit.MILLISECONDS);

            Counter slowQueryCounter = getOrCreateSlowQueryCounter(commandName, databaseName, clusterId);
            slowQueryCounter.increment();
        }

        updateRates();
    }

    @Override
        public void commandFailed(CommandFailedEvent commandFailedEvent) {
        commandErrorCounter.increment();
        log.info("mongo command failed, {}", commandFailedEvent.getThrowable().getMessage());

        String commandName = commandFailedEvent.getCommandName();
        String databaseName = commandFailedEvent.getDatabaseName();
        ConnectionId connectionId = commandFailedEvent.getConnectionDescription().getConnectionId();
        String clusterId = connectionId.getServerId().getClusterId().getValue();
        String msg = commandFailedEvent.getThrowable().getMessage();

        Counter commandFailedCounter = getOrCreateCommandFailedCounter(commandName, databaseName, clusterId, msg);
        commandFailedCounter.increment();
    }

    private void updateRates() {
        oneMinuteCounter.incrementAndGet();
        fiveMinuteCounter.incrementAndGet();
        fifteenMinuteCounter.incrementAndGet();

        defaultMeter.setOneMinuteRate(oneMinuteCounter.get());
        defaultMeter.setFiveMinuteRate(fiveMinuteCounter.get() / 5.0);
        defaultMeter.setFifteenMinuteRate(fifteenMinuteCounter.get() / 15.0);

        // Reset the counters if needed to avoid overflow
        if (oneMinuteCounter.get() >= 60) {
            oneMinuteCounter.set(0);
        }
        if (fiveMinuteCounter.get() >= 300) {
            fiveMinuteCounter.set(0);
        }
        if (fifteenMinuteCounter.get() >= 900) {
            fifteenMinuteCounter.set(0);
        }
    }

    private void registerGauges() {
        Gauge.builder("mongodb.command.m1.count", defaultMeter, dm -> (long) (dm.getOneMinuteRate() * 60))
                    .description("1 minute count")
                    .register(meterRegistry);

        Gauge.builder("mongodb.command.m5.count", defaultMeter, dm -> (long) (dm.getFiveMinuteRate() * 60 * 5))
                    .description("5 minute count")
                    .register(meterRegistry);

        Gauge.builder("mongodb.command.m15.count", defaultMeter, dm -> (long) (dm.getFifteenMinuteRate() * 60 * 15))
                    .description("15 minute count")
                    .register(meterRegistry);
    }

}