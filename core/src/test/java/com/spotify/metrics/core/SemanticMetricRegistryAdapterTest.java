package com.spotify.metrics.core;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SemanticMetricRegistryAdapterTest {

    @Mock
    private Counter counter;

    @Mock
    private Gauge<Integer> gauge;

    @Mock
    private Histogram histogram;

    @Mock
    private Meter meter;

    @Mock
    private Timer timer;

    @Mock
    private SemanticMetricRegistry target;

    private MetricRegistry source;

    @Before
    public void setUp() throws Exception {
        SemanticMetricIdAdapter idAdapter = new SemanticMetricIdAdapter() {
            @Override
            public MetricId buildMetricId(final String metricName) {
                return MetricId.build(metricName);
            }
        };

        this.source = SemanticMetricRegistryAdapter.adaptingMetricRegistry(target, idAdapter);
    }

    @Test
    public void addsAndRemovesCounters() throws Exception {
        source.register("a", counter);
        verify(target).register(MetricId.build("a"), counter);
        source.remove("a");
        verify(target).remove(MetricId.build("a"));
    }

    @Test
    public void addsAndRemovesGauges() throws Exception {
        source.register("b", gauge);
        verify(target).register(MetricId.build("b"), gauge);
        source.remove("b");
        verify(target).remove(MetricId.build("b"));
    }

    @Test
    public void addsAndRemovesHistograms() throws Exception {
        source.register("c", histogram);
        verify(target).register(MetricId.build("c"), histogram);
        source.remove("c");
        verify(target).remove(MetricId.build("c"));
    }

    @Test
    public void addsAndRemovesMeters() throws Exception {
        source.register("d", meter);
        verify(target).register(MetricId.build("d"), meter);
        source.remove("d");
        verify(target).remove(MetricId.build("d"));
    }

    @Test
    public void addsAndRemovesTimers() throws Exception {
        source.register("e", timer);
        verify(target).register(MetricId.build("e"), timer);
        source.remove("e");
        verify(target).remove(MetricId.build("e"));
    }

    @Test
    public void silentlyIgnoresExceptions() throws Exception {
        doThrow(new RuntimeException("a"))
            .when(target)
            .register(any(MetricId.class), any(Metric.class));
        source.register("f", gauge);
        // No exception thrown
    }
}
