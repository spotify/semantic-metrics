package com.spotify.metrics.core;

import static org.junit.Assert.*;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;

public class SemanticMetricBuilderFactoryTest {

    @Test
    public void timerWithReservoir() throws Exception {
        final SemanticMetricBuilder<Timer> result =
            SemanticMetricBuilderFactory.timerWithReservoir(() -> {
                return LockFreeExponentiallyDecayingReservoir.builder().build();
            });
        assert Timer.class.isInstance(result.newMetric());
    }

    @Test
    public void histogramWithReservoir() throws Exception {
        final SemanticMetricBuilder<Histogram> result =
            SemanticMetricBuilderFactory.histogramWithReservoir(() -> {
                return LockFreeExponentiallyDecayingReservoir.builder().build();
            });
        assert Histogram.class.isInstance(result.newMetric());
    }

    @Test
    public void gauge() {
        final SemanticMetricBuilder<Gauge<Double>> result = SemanticMetricBuilderFactory.gauge(() -> () -> 123.0);
        final Gauge<Double> instance = result.newMetric();
        assertEquals(123.0, instance.getValue(), 0.01);
    }
}