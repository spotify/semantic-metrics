package com.spotify.metrics.ffwd;

import com.codahale.metrics.Gauge;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.spotify.metrics.core.DerivingMeter;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.tags.EnvironmentTagExtractor;
import eu.toolchain.ffwd.FastForward;
import eu.toolchain.ffwd.Metric;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FastForwardReporterTest {
    private static final int REPORTING_PERIOD = 50;
    FastForwardReporter reporter;

    SemanticMetricRegistry registry;
    FastForward fastForward;

    @Before
    public void setUp() throws Exception {
        registry = new SemanticMetricRegistry();
        fastForward = mock(FastForward.class);
        reporter = FastForwardReporter
            .forRegistry(registry)
            .schedule(TimeUnit.MILLISECONDS, REPORTING_PERIOD)
            .fastForward(fastForward)
            .build();
    }

    @Test
    public void shouldReportPeriodicallyWhenStarted() throws Exception {
        registry.counter(MetricId.build("hi"));
        reporter.start();

        verify(fastForward, timeout(REPORTING_PERIOD * 2 + 20).atLeast(2)).send(any(Metric.class));
    }

    @Test
    public void shouldStopReportingAfterStop() throws Exception {
        registry.counter(MetricId.build("hi"));
        final AtomicInteger counter = new AtomicInteger(0);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                counter.incrementAndGet();
                return null;
            }
        }).when(fastForward).send(any(Metric.class));

        reporter.start();

        Thread.sleep(REPORTING_PERIOD);

        reporter.stop();

        Thread.sleep(REPORTING_PERIOD);

        int shouldHaveStoppedNow = counter.get();

        Thread.sleep(REPORTING_PERIOD * 2);

        assertThat(counter.get(), equalTo(shouldHaveStoppedNow));
    }

    @Test
    public void shouldIncludeDerivingMetersInReport() throws Exception {
        registry.counter(MetricId.build("hi"));
        ArgumentCaptor<Metric> argumentCaptor = ArgumentCaptor.forClass(Metric.class);

        doNothing().when(fastForward).send(argumentCaptor.capture());

        MetricId name = MetricId.build("thename");

        DerivingMeter derivingMeter = registry.derivingMeter(name);

        derivingMeter.mark(0);
        derivingMeter.mark(666);

        reporter.start();

        verify(fastForward, timeout(REPORTING_PERIOD + REPORTING_PERIOD / 3).atLeastOnce()).send(
            any(Metric.class));

        List<Metric> metrics = argumentCaptor.getAllValues();

        // initialise a modifiable map with all entries flagged as 'not found'
        Map<String, Boolean> foundStats =
            new HashMap<>(Maps.asMap(ImmutableSet.of("1m", "5m"), new Function<String, Boolean>() {
                @Override
                public Boolean apply(String input) {
                    return false;
                }
            }));

        for (Metric metric : metrics) {
            if (metric.getKey().equals("thename")) {
                String stat = metric.getAttributes().get("stat");

                foundStats.put(stat, true);
            }
        }

        for (String stat : foundStats.keySet()) {
            assertThat("found " + stat, foundStats.get(stat), is(true));
        }
    }

    @Test
    public void shouldAddExtractedTags() throws Exception {
        registry.counter(MetricId.build("hi"));
        final Map<String, String> tags = ImmutableMap.of("FFWD_TAG_foo", "bar");

        final Supplier<Map<String, String>> environmentSupplier =
            Suppliers.ofInstance(tags);

        reporter = FastForwardReporter
            .forRegistry(registry)
            .schedule(TimeUnit.MILLISECONDS, REPORTING_PERIOD)
            .fastForward(fastForward)
            .tagExtractor(new EnvironmentTagExtractor(environmentSupplier))
            .build();

        ArgumentCaptor<Metric> argument = ArgumentCaptor.forClass(Metric.class);

        MetricId name = MetricId.build("thename");

        final com.codahale.metrics.Counter counter = registry.counter(name);
        counter.inc(1);
        reporter.start();

        verify(fastForward, timeout(REPORTING_PERIOD + REPORTING_PERIOD / 3)
            .atLeastOnce())
            .send(argument.capture());

        final ImmutableMap<String, String> expected =
            ImmutableMap.of("metric_type", "counter", "foo", "bar");

        assertEquals(expected, argument.getValue().getAttributes());
    }

    @Test
    public void testEmitGaugeValue() throws IOException {
        registry.register(MetricId.EMPTY.tagged("testcase", "test-emit-gauge-value"), new Gauge<Double>() {
            @Override
            public Double getValue() {
                return 123D;
            }
        });
        reporter.report();
        ArgumentCaptor<Metric> argument = ArgumentCaptor.forClass(Metric.class);
        verify(fastForward).send(argument.capture());
        assertEquals(123D, argument.getValue().getValue());
    }

    @Test
    public void testEmitGaugeDefaultValue() throws IOException {
        reporter = FastForwardReporter
                .forRegistry(registry)
                .schedule(TimeUnit.MILLISECONDS, REPORTING_PERIOD)
                .fastForward(fastForward)
                .alwaysEmitGauges(true)
                .build();

        registry.register(MetricId.EMPTY.tagged("testcase", "test-emit-gauge-default"), new Gauge<Double>() {
            @Override
            public Double getValue() {
                return null;
            }
        });
        reporter.report();
        ArgumentCaptor<Metric> argument = ArgumentCaptor.forClass(Metric.class);
        verify(fastForward).send(argument.capture());
        assertEquals(0D, argument.getValue().getValue());
    }

    @Test
    public void testDontEmitGauge() throws IOException {
        reporter = FastForwardReporter
                .forRegistry(registry)
                .schedule(TimeUnit.MILLISECONDS, REPORTING_PERIOD)
                .fastForward(fastForward)
                .alwaysEmitGauges(false)
                .build();

        registry.register(MetricId.EMPTY.tagged("testcase", "test-dont-emit-gauge"), new Gauge<Double>() {
            @Override
            public Double getValue() {
                return null;
            }
        });
        reporter.report();

        verifyZeroInteractions(fastForward);
    }
}