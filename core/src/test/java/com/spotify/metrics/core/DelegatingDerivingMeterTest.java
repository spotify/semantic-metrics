package com.spotify.metrics.core;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Meter;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DelegatingDerivingMeterTest {
    DelegatingDerivingMeter meter;

    Meter delegate;

    @Before
    public void setUp() throws Exception {
        delegate = new Meter();

        meter = new DelegatingDerivingMeter(delegate);
    }

    @Test
    public void shouldNotCountSingleValue() throws Exception {
        meter.mark(10);

        assertThat(meter.getCount(), equalTo(0L));
    }

    @Test
    public void shouldCountDeltasNotAbsolutes() throws Exception {
        meter.mark(10);
        meter.mark(20);
        meter.mark(30);

        assertThat(meter.getCount(), equalTo(20L));
    }

    @Test
    public void shouldDiscardWraparounds() throws Exception {
        meter.mark(100);
        meter.mark(20);
        meter.mark(30);

        assertThat(meter.getCount(), equalTo(10L));
    }

    @Test
    public void shouldReturnValueFromDelegateForMeanRate() throws Exception {
        setupRateTest();

        assertThat(meter.getMeanRate(), is(greaterThan(300.0)));
    }

    @Test
    public void shouldReturnValueFromDelegateForOneMinuteRate() throws Exception {
        setupRateTest();

        assertThat(meter.getOneMinuteRate(), is(greaterThan(300.0)));
    }

    @Test
    public void shouldReturnValueFromDelegateForFiveMinuteRate() throws Exception {
        setupRateTest();

        assertThat(meter.getFiveMinuteRate(), is(greaterThan(300.0)));
    }

    @Test
    public void shouldReturnValueFromDelegateForFifteenMinuteRate() throws Exception {
        setupRateTest();

        assertThat(meter.getFifteenMinuteRate(), is(greaterThan(300.0)));
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

    private Matcher<Double> greaterThan(final double expected) {
        return new TypeSafeMatcher<Double>() {
            @Override
            protected boolean matchesSafely(Double item) {
                return item > expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("double greater than " + expected);
            }
        };
    }
}