package com.spotify.metrics.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class InstrumentedScheduledExecutorServiceTest {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(InstrumentedScheduledExecutorServiceTest.class);

    private Meter submitted;
    private Counter running;
    private Meter completed;
    private Timer duration;
    private Meter scheduledOnce;
    private Meter scheduledRepetitively;
    private Counter scheduledOverrun;
    private Histogram percentOfPeriod;
    private InstrumentedScheduledExecutorService instrumentedScheduledExecutor;

    @Parameterized.Parameters(name = "using default base metric id: {0}")
    public static Object[] parameters() {
        return new Object[]{
            true, false
        };
    }

    @Parameterized.Parameter
    public boolean useDefaultBaseMetricId;

    @Rule
    public final TestRule globalTimeout = Timeout.seconds(2);
    private static final AtomicLong EXECUTOR_NAME_PREFIX = new AtomicLong();

    @Before
    public void setUp() {
        final SemanticMetricRegistry registry = new SemanticMetricRegistry();
        MetricId baseMetricId;
        if (useDefaultBaseMetricId) {
            instrumentedScheduledExecutor = new InstrumentedScheduledExecutorService(
                Executors.newSingleThreadScheduledExecutor(), registry);
            baseMetricId = new MetricId().tagged("executor",
                "scheduled-executor-" + EXECUTOR_NAME_PREFIX.incrementAndGet());
        } else {
            baseMetricId = new MetricId().tagged("executor", "xs");
            instrumentedScheduledExecutor = new InstrumentedScheduledExecutorService(
                Executors.newSingleThreadScheduledExecutor(), registry, baseMetricId);
        }
        final MetricId baseMetricIdWithUnit = baseMetricId.tagged("unit", "task");
        submitted = registry.meter(baseMetricIdWithUnit.tagged("what", "submitted"));
        running = registry.counter(baseMetricIdWithUnit.tagged("what", "running"));
        completed = registry.meter(baseMetricIdWithUnit.tagged("what", "completed"));
        duration = registry.timer(baseMetricIdWithUnit.tagged("what", "duration"));
        scheduledOnce = registry.meter(baseMetricIdWithUnit.tagged("what", "scheduled.once"));
        scheduledRepetitively =
            registry.meter(baseMetricIdWithUnit.tagged("what", "scheduled.repetitively"));
        scheduledOverrun =
            registry.counter(baseMetricIdWithUnit.tagged("what", "scheduled.overrun"));
        percentOfPeriod =
            registry.histogram(baseMetricIdWithUnit.tagged("what", "scheduled.percent-of-period"));
    }

    @After
    public void tearDown() throws Exception {
        instrumentedScheduledExecutor.shutdown();
        if (!instrumentedScheduledExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
            LOGGER.error("InstrumentedScheduledExecutorService did not terminate.");
        }
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(0, completed.getCount());
        assertEquals(0, duration.getCount());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());

        Future<?> theFuture = instrumentedScheduledExecutor.submit(new Runnable() {
            @Override
            public void run() {
                assertEquals(1, submitted.getCount());
                assertEquals(1, running.getCount());
                assertEquals(0, completed.getCount());
                assertEquals(0, duration.getCount());
                assertEquals(0, scheduledOnce.getCount());
                assertEquals(0, scheduledRepetitively.getCount());
                assertEquals(0, scheduledOverrun.getCount());
                assertEquals(0, percentOfPeriod.getCount());
            }
        });

        theFuture.get();

        assertEquals(1, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(1, completed.getCount());
        assertEquals(1, duration.getCount());
        assertEquals(1, duration.getSnapshot().size());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());
    }

    @Test
    public void testScheduleRunnable() throws Exception {
        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(0, completed.getCount());
        assertEquals(0, duration.getCount());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());

        ScheduledFuture<?> theFuture = instrumentedScheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                assertEquals(0, submitted.getCount());
                assertEquals(1, running.getCount());
                assertEquals(0, completed.getCount());
                assertEquals(0, duration.getCount());
                assertEquals(1, scheduledOnce.getCount());
                assertEquals(0, scheduledRepetitively.getCount());
                assertEquals(0, scheduledOverrun.getCount());
                assertEquals(0, percentOfPeriod.getCount());
            }
        }, 10L, TimeUnit.MILLISECONDS);

        theFuture.get();

        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(1, completed.getCount());
        assertEquals(1, duration.getCount());
        assertEquals(1, duration.getSnapshot().size());
        assertEquals(1, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());
    }

    @Test
    public void testSubmitCallable() throws Exception {
        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(0, completed.getCount());
        assertEquals(0, duration.getCount());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());

        final Object obj = new Object();

        Future<Object> theFuture = instrumentedScheduledExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() {
                assertEquals(1, submitted.getCount());
                assertEquals(1, running.getCount());
                assertEquals(0, completed.getCount());
                assertEquals(0, duration.getCount());
                assertEquals(0, scheduledOnce.getCount());
                assertEquals(0, scheduledRepetitively.getCount());
                assertEquals(0, scheduledOverrun.getCount());
                assertEquals(0, percentOfPeriod.getCount());
                return obj;
            }
        });

        assertEquals(obj, theFuture.get());

        assertEquals(1, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(1, completed.getCount());
        assertEquals(1, duration.getCount());
        assertEquals(1, duration.getSnapshot().size());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());
    }

    @Test
    public void testScheduleCallable() throws Exception {
        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(0, completed.getCount());
        assertEquals(0, duration.getCount());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());

        final Object obj = new Object();

        ScheduledFuture<Object> theFuture =
            instrumentedScheduledExecutor.schedule(new Callable<Object>() {
                @Override
                public Object call() {

                    assertEquals(0, submitted.getCount());
                    assertEquals(1, running.getCount());
                    assertEquals(0, completed.getCount());
                    assertEquals(0, duration.getCount());
                    assertEquals(1, scheduledOnce.getCount());
                    assertEquals(0, scheduledRepetitively.getCount());
                    assertEquals(0, scheduledOverrun.getCount());
                    assertEquals(0, percentOfPeriod.getCount());

                    return obj;
                }
            }, 10L, TimeUnit.MILLISECONDS);

        assertEquals(obj, theFuture.get());

        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(1, completed.getCount());
        assertEquals(1, duration.getCount());
        assertEquals(1, duration.getSnapshot().size());
        assertEquals(1, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());
    }

    @Test
    public void testScheduleFixedRateRunnable() throws Exception {
        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertEquals(0, completed.getCount());
        assertEquals(0, duration.getCount());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());

        ScheduledFuture<?> theFuture =
            instrumentedScheduledExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    assertEquals(0, submitted.getCount());
                    assertEquals(1, running.getCount());
                    assertEquals(0, scheduledOnce.getCount());
                    assertEquals(1, scheduledRepetitively.getCount());
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, 10L, 10L, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(100);
        theFuture.cancel(true);
        TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertNotEquals(0, completed.getCount());
        assertNotEquals(0, duration.getCount());
        assertNotEquals(0, duration.getSnapshot().size());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(1, scheduledRepetitively.getCount());
        assertNotEquals(0, scheduledOverrun.getCount());
        assertNotEquals(0, percentOfPeriod.getCount());
    }

    @Test
    public void testScheduleFixedDelayRunnable() throws Exception {
        assertEquals(0, submitted.getCount());

        assertEquals(0, running.getCount());
        assertEquals(0, completed.getCount());
        assertEquals(0, duration.getCount());
        assertEquals(0, scheduledOnce.getCount());
        assertEquals(0, scheduledRepetitively.getCount());
        assertEquals(0, scheduledOverrun.getCount());
        assertEquals(0, percentOfPeriod.getCount());

        ScheduledFuture<?> theFuture =
            instrumentedScheduledExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    assertEquals(0, submitted.getCount());
                    assertEquals(1, running.getCount());
                    assertEquals(0, scheduledOnce.getCount());
                    assertEquals(1, scheduledRepetitively.getCount());

                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, 10L, 10L, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(100);
        theFuture.cancel(true);
        TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(0, submitted.getCount());
        assertEquals(0, running.getCount());
        assertNotEquals(0, completed.getCount());
        assertNotEquals(0, duration.getCount());
        assertNotEquals(0, duration.getSnapshot().size());
    }
}