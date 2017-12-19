package com.spotify.metrics.ffwd;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableSet;
import com.spotify.ffwd.http.Batch;
import com.spotify.ffwd.http.HttpClient;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwdhttp.Clock;
import com.spotify.metrics.ffwdhttp.FastForwardHttpReporter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Completable;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class FastForwardReporterTest {
    private static final int REPORTING_PERIOD = 50;
    public static final long TIME = 42L;
    private FastForwardHttpReporter reporter;

    private SemanticMetricRegistry registry;
    private Clock.Fixed fixedClock;
    @Mock
    private HttpClient httpClient;
    @Mock
    private Observable<Void> observable;
    @Mock
    private Completable completable;
    private Map<String, String> commonTags;

    @Before
    public void setUp() throws Exception {
        registry = new SemanticMetricRegistry();
        fixedClock = new Clock.Fixed(0L);

        commonTags = of("foo", "bar");

        reporter = FastForwardHttpReporter
            .forRegistry(registry, httpClient)
            .schedule(REPORTING_PERIOD, TimeUnit.MILLISECONDS)
            .prefix(MetricId.build("prefix").tagged(commonTags))
            .clock(fixedClock)
            .build();
    }

    @Test
    public void someReporting() throws Exception {
        doReturn(Observable.<Void>just(null)).when(httpClient).sendBatch(any(Batch.class));
        fixedClock.setCurrentTime(TIME);

        registry.counter(MetricId.build("counter"));
        registry.derivingMeter(MetricId.build("deriving-meter"));
        registry.histogram(MetricId.build("histogram"));
        registry.meter(MetricId.build("meter"));
        registry.timer(MetricId.build("timer"));
        registry.register(MetricId.build("gauge").tagged("what", "some-gauge"),
            new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return 0D;
                }
            });

        final Set<Batch.Point> expected = new HashSet<>();
        expected.add(new Batch.Point("prefix.counter", of("unit", "n", "stat", "count"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.deriving-meter", of("unit", "n/s", "stat", "5m"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.deriving-meter", of("unit", "n/s", "stat", "1m"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram", of("unit", "n", "stat", "max"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram", of("unit", "n", "stat", "min"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram", of("unit", "n", "stat", "mean"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram", of("unit", "n", "stat", "p75"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.histogram", of("unit", "n", "stat", "median"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.histogram", of("unit", "n", "stat", "stddev"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram", of("unit", "n", "stat", "p99"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter", of("unit", "n", "stat", "count"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter", of("unit", "n/s", "stat", "1m"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter", of("unit", "n/s", "stat", "5m"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "max"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "min"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "mean"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "p75"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "median"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "stddev"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns", "stat", "p99"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns/s", "stat", "1m"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer", of("unit", "ns/s", "stat", "5m"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.gauge", of("what", "some-gauge", "unit", "n"), 0, TIME));

        reporter.start();

        final ArgumentCaptor<Batch> batch = ArgumentCaptor.forClass(Batch.class);

        verify(httpClient, timeout(REPORTING_PERIOD * 2 + 20).atLeast(2)).sendBatch(
            batch.capture());

        for (final Batch b : batch.getAllValues()) {
            assertEquals(commonTags, b.getCommonTags());
            final Set<Batch.Point> points = new HashSet<>(b.getPoints());
            points.removeAll(expected);
            assertEquals("expected empty set of points", ImmutableSet.of(), points);
        }
    }
}