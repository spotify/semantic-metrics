package com.spotify.metrics.ffwd;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Gauge;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.ffwd.http.Batch;
import com.spotify.ffwd.http.HttpClient;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwdhttp.Clock;
import com.spotify.metrics.ffwdhttp.FastForwardHttpReporter;
import com.spotify.metrics.tags.EnvironmentTagExtractor;
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
        registry.meter(MetricId.build("meter").tagged("unit", "spec"));
        registry.meter(MetricId.build("meter2"));
        registry.timer(MetricId.build("timer"));
        registry.register(MetricId.build("gauge").tagged("what", "some-gauge"),
            new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return 0D;
                }
            });

        final Set<Batch.Point> expected = new HashSet<>();
        expected.add(new Batch.Point("prefix.counter",
            of("unit", "n", "stat", "count", "metric_type", "counter"), 0, TIME));
        expected.add(new Batch.Point("prefix.deriving-meter",
            of("unit", "n/s", "stat", "5m", "metric_type", "deriving-meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.deriving-meter",
            of("unit", "n/s", "stat", "1m", "metric_type", "deriving-meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "max", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "min", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "mean", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "p75", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "median", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "stddev", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.histogram",
            of("unit", "n", "stat", "p99", "metric_type", "histogram"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter",
            of("unit", "spec", "stat", "count", "metric_type", "meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter",
            of("unit", "spec/s", "stat", "1m", "metric_type", "meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter",
            of("unit", "spec/s", "stat", "5m", "metric_type", "meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter2",
            of("unit", "n", "stat", "count", "metric_type", "meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter2",
            of("unit", "n/s", "stat", "1m", "metric_type", "meter"), 0, TIME));
        expected.add(new Batch.Point("prefix.meter2",
            of("unit", "n/s", "stat", "5m", "metric_type", "meter"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.timer", of("unit", "ns", "stat", "max", "metric_type", "timer"),
                0, TIME));
        expected.add(
            new Batch.Point("prefix.timer", of("unit", "ns", "stat", "min", "metric_type", "timer"),
                0, TIME));
        expected.add(new Batch.Point("prefix.timer",
            of("unit", "ns", "stat", "mean", "metric_type", "timer"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.timer", of("unit", "ns", "stat", "p75", "metric_type", "timer"),
                0, TIME));
        expected.add(new Batch.Point("prefix.timer",
            of("unit", "ns", "stat", "median", "metric_type", "timer"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer",
            of("unit", "ns", "stat", "stddev", "metric_type", "timer"), 0, TIME));
        expected.add(
            new Batch.Point("prefix.timer", of("unit", "ns", "stat", "p99", "metric_type", "timer"),
                0, TIME));
        expected.add(new Batch.Point("prefix.timer",
            of("unit", "ns/s", "stat", "1m", "metric_type", "timer"), 0, TIME));
        expected.add(new Batch.Point("prefix.timer",
            of("unit", "ns/s", "stat", "5m", "metric_type", "timer"), 0, TIME));
        expected.add(new Batch.Point("prefix.gauge",
            of("what", "some-gauge", "unit", "n", "metric_type", "gauge"), 0, TIME));

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

    @Test
    public void shouldAddExtractedTags() throws Exception {
        final Map<String, String> tags = ImmutableMap.of("FFWD_TAG_bar", "baz");

        final Supplier<Map<String, String>> environmentSupplier =
            Suppliers.ofInstance(tags);

        reporter = FastForwardHttpReporter
            .forRegistry(registry, httpClient)
            .schedule(REPORTING_PERIOD, TimeUnit.MILLISECONDS)
            .prefix(MetricId.build("prefix").tagged(commonTags))
            .clock(fixedClock)
            .tagExtractor(new EnvironmentTagExtractor(environmentSupplier))
            .build();

        doReturn(Observable.<Void>just(null)).when(httpClient).sendBatch(any(Batch.class));
        fixedClock.setCurrentTime(TIME);

        reporter.start();

        final ArgumentCaptor<Batch> batch = ArgumentCaptor.forClass(Batch.class);

        verify(httpClient, timeout(REPORTING_PERIOD * 2 + 20).atLeast(2)).sendBatch(
            batch.capture());

        final Map<String, String> commonTags = batch.getValue().getCommonTags();
        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz"), commonTags);
    }
}