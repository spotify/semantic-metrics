package com.spotify.metrics.ffwd;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.spotify.ffwd.FastForward;
import com.spotify.ffwd.Metric;
import com.spotify.metrics.core.DerivingMeter;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.tags.EnvironmentTagExtractor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jmock.lib.concurrent.DeterministicScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class FastForwardReporterTest {
    private static final int REPORTING_PERIOD = 50;
    FastForwardReporter reporter;

    SemanticMetricRegistry registry;
    FastForward fastForward;
    DeterministicScheduler executorService;

    @Before
    public void setUp() throws Exception {
        registry = new SemanticMetricRegistry();
        fastForward = mock(FastForward.class);
        executorService = new DeterministicScheduler();
        reporter = FastForwardReporter
            .forRegistry(registry)
            .prefix("test")
            .schedule(TimeUnit.MILLISECONDS, REPORTING_PERIOD)
            .fastForward(fastForward)
            .executorService(executorService)
            .build();

        registry.counter(MetricId.build("hi"));
    }

    @Test
    public void shouldReportPeriodicallyWhenStarted() throws Exception {
        reporter.start();

        executorService.tick(REPORTING_PERIOD * 2 + 20, TimeUnit.MILLISECONDS);
        verify(fastForward, atLeast(2)).send(any(Metric.class));
    }

    @Test
    public void shouldStopReportingAfterStop() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(fastForward).send(any(Metric.class));

        reporter.start();

        executorService.tick(REPORTING_PERIOD, TimeUnit.MILLISECONDS);

        reporter.stop();

        executorService.tick(REPORTING_PERIOD, TimeUnit.MILLISECONDS);

        int shouldHaveStoppedNow = counter.get();

        executorService.tick(REPORTING_PERIOD * 2, TimeUnit.MILLISECONDS);

        assertEquals(counter.get(), shouldHaveStoppedNow);
    }

    @Test
    public void shouldIncludeDerivingMetersInReport() throws Exception {
        ArgumentCaptor<Metric> argumentCaptor = ArgumentCaptor.forClass(Metric.class);

        doNothing().when(fastForward).send(argumentCaptor.capture());

        MetricId name = MetricId.build("thename");

        DerivingMeter derivingMeter = registry.derivingMeter(name);

        derivingMeter.mark(0);
        derivingMeter.mark(666);

        reporter.start();

        executorService.tick(REPORTING_PERIOD + REPORTING_PERIOD / 3, TimeUnit.MILLISECONDS);
        verify(fastForward, atLeastOnce()).send(any(Metric.class));

        List<Metric> metrics = argumentCaptor.getAllValues();

        // initialise a modifiable map with all entries flagged as 'not found'
        Map<String, Boolean> foundStats =
            new HashMap<>(Maps.asMap(ImmutableSet.of("1m", "5m"), input -> false));

        for (Metric metric : metrics) {
            if (metric.getKey().equals("test.thename")) {
                String stat = metric.getAttributes().get("stat");

                foundStats.put(stat, true);
            }
        }

        for (String stat : foundStats.keySet()) {
            assertEquals("found " + stat, foundStats.get(stat), true);
        }
    }

    @Test
    public void shouldAddExtractedTags() throws Exception {
        final Map<String, String> tags = ImmutableMap.of("FFWD_TAG_foo", "bar");

        final Supplier<Map<String, String>> environmentSupplier =
            Suppliers.ofInstance(tags);

        reporter = FastForwardReporter
            .forRegistry(registry)
            .schedule(TimeUnit.MILLISECONDS, REPORTING_PERIOD)
            .fastForward(fastForward)
            .tagExtractor(new EnvironmentTagExtractor(environmentSupplier))
            .executorService(executorService)
            .build();

        ArgumentCaptor<Metric> argument = ArgumentCaptor.forClass(Metric.class);

        MetricId name = MetricId.build("thename");

        final com.codahale.metrics.Counter counter = registry.counter(name);
        counter.inc(1);
        reporter.start();

        executorService.tick(REPORTING_PERIOD + REPORTING_PERIOD / 3, TimeUnit.MILLISECONDS);

        verify(fastForward, atLeastOnce()).send(argument.capture());

        final ImmutableMap<String, String> expected =
            ImmutableMap.of("metric_type", "counter", "foo", "bar");

        assertEquals(expected, argument.getValue().getAttributes());
    }

    @Test
    public void shouldNotReportPeriodicallyNoTick() throws Exception {
        reporter.start();

        verify(fastForward, never()).send(any(Metric.class));
    }

    @Test
    public void shouldNotFinalReportAfterStop() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(fastForward).send(any(Metric.class));

        reporter.start();

        executorService.tick(REPORTING_PERIOD, TimeUnit.MILLISECONDS);

        int counterBeforeStop = counter.get();

        reporter.stop();

        executorService.tick(REPORTING_PERIOD, TimeUnit.MILLISECONDS);

        int counterAfterStop = counter.get();

        executorService.tick(REPORTING_PERIOD * 2, TimeUnit.MILLISECONDS);

        assertEquals(counterBeforeStop, counterAfterStop);
    }

    @Test
    public void shouldFinalReportAfterStopWithFlush() throws Exception {
        final AtomicInteger counter = new AtomicInteger(0);

        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(fastForward).send(any(Metric.class));

        reporter.start();

        executorService.tick(REPORTING_PERIOD, TimeUnit.MILLISECONDS);

        int counterBeforeStop = counter.get();

        reporter.stopWithFlush();

        executorService.tick(REPORTING_PERIOD, TimeUnit.MILLISECONDS);

        int counterAfterStop = counter.get();

        executorService.tick(REPORTING_PERIOD * 2, TimeUnit.MILLISECONDS);

        assert(counterBeforeStop < counterAfterStop);
    }

    @Test
    public void testKeyValuePrefixAddedOnce() throws Exception {
        ArgumentCaptor<Metric> argumentCaptor = ArgumentCaptor.forClass(Metric.class);

        doNothing().when(fastForward).send(argumentCaptor.capture());

        MetricId name = MetricId.build("thename");

        registry.meter(name);

        reporter.start();

        executorService.tick(REPORTING_PERIOD + REPORTING_PERIOD / 3, TimeUnit.MILLISECONDS);
        verify(fastForward, atLeastOnce()).send(any(Metric.class));

        Set<String> expectedKeys = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "test.hi", "test.thename", "test.thename", "test.thename", "test.hi", "test.thename",
        "test.thename", "test.thename")));

        Set<String> actualKeys = argumentCaptor.getAllValues().stream().map(Metric::getKey)
        .collect(Collectors.toSet());

        assertEquals(expectedKeys, actualKeys);
    }

    @Test
    public void shouldNotReportEmptyHistogramsAndTimers() throws Exception {
        ArgumentCaptor<Metric> argumentCaptor = ArgumentCaptor.forClass(Metric.class);

        doNothing().when(fastForward).send(argumentCaptor.capture());

        MetricId name = MetricId.build("thename");

        registry.histogram(name.tagged("histogram", "true"));
        registry.timer(name.tagged("timer", "true"));

        reporter.start();

        executorService.tick(REPORTING_PERIOD + REPORTING_PERIOD / 3, TimeUnit.MILLISECONDS);
        verify(fastForward, atLeastOnce()).send(any(Metric.class));

        Set<String> actualKeys = argumentCaptor.getAllValues().stream().map(Metric::getKey)
                .collect(Collectors.toSet());

        assertEquals(new HashSet<>(Arrays.asList("test.hi")), actualKeys);
    }
}
