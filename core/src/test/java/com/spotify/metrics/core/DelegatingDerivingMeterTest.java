package com.spotify.metrics.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Meter;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class DelegatingDerivingMeterTest {
    private DelegatingDerivingMeter meter;

    private Meter delegate;

    @Before
    public void setUp() throws Exception {
        delegate = new Meter();

        meter = new DelegatingDerivingMeter(delegate);
    }

    @Test
    public void shouldNotCountSingleValue() throws Exception {
        meter.mark(10);

        assertEquals(meter.getCount(), 0L);
    }

    @Test
    public void shouldCountDeltasNotAbsolutes() throws Exception {
        meter.mark(10);
        meter.mark(20);
        meter.mark(30);

        assertEquals(meter.getCount(), 20L);
    }

    @Test
    public void shouldDiscardWraparounds() throws Exception {
        meter.mark(100);
        meter.mark(20);
        meter.mark(30);

        assertEquals(meter.getCount(), 10L);
    }

    @Test
    public void shouldReturnValueFromDelegateForMeanRate() throws Exception {
        setupRateTest();

        assertTrue(meter.getMeanRate()> 300.0);
    }

    @Test
    public void shouldReturnValueFromDelegateForOneMinuteRate() throws Exception {
        setupRateTest();

        assertTrue(meter.getOneMinuteRate() > 300.0);
    }

    @Test
    public void shouldReturnValueFromDelegateForFiveMinuteRate() throws Exception {
        setupRateTest();

        assertTrue(meter.getFiveMinuteRate() > 300.0);
    }

    @Test
    public void shouldReturnValueFromDelegateForFifteenMinuteRate() throws Exception {
        setupRateTest();

        assertTrue(meter.getFifteenMinuteRate() > 300.0);
    }

    private void setupRateTest() throws InterruptedException {
        Clock clock = mock(Clock.class);

        meter = new DelegatingDerivingMeter(new Meter(clock));

        when(clock.getTick()).thenReturn(0L);
        meter.mark(0);
        meter.mark(2000);
        // need to 'wait' more than 5 seconds for the Codahale metrics meter to 'tick'.
        when(clock.getTick()).thenReturn(TimeUnit.NANOSECONDS.convert(6, TimeUnit.SECONDS));
    }
}
