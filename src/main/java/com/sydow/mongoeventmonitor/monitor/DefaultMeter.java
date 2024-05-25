package com.sydow.mongoeventmonitor.monitor;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Statistic;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class DefaultMeter implements Meter {

    @Setter
    @Getter
    private double oneMinuteRate;
    @Setter
    @Getter
    private double fiveMinuteRate;
    @Setter
    @Getter
    private double fifteenMinuteRate;

    public DefaultMeter(Meter.Id id, double oneMinuteRate, double fiveMinuteRate, double fifteenMinuteRate) {
        super();
        this.oneMinuteRate = oneMinuteRate;
        this.fiveMinuteRate = fiveMinuteRate;
        this.fifteenMinuteRate = fifteenMinuteRate;
    }

    @Override
    public Id getId() {
        return null;
    }

    @Override
    public Iterable<Measurement> measure() {
        return List.of(
            new Measurement(() -> oneMinuteRate, Statistic.VALUE),
            new Measurement(() -> fiveMinuteRate, Statistic.VALUE),
            new Measurement(() -> fifteenMinuteRate, Statistic.VALUE)
        );
    }

}