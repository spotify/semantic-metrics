package com.spotify.metrics;

import com.spotify.metrics.core.DerivedLongGauge;
import com.spotify.metrics.core.DerivedLongGauge.TimeProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicLong;

public class DerivedLongGaugeTest {
    private static TimeProvider a = Mockito.mock(TimeProvider.class);

    private final AtomicLong value = new AtomicLong();

    @Test
    public void testBasic() {
        Mockito.when(a.currentTime()).thenReturn(0l);

        DerivedLongGauge gauge = new DerivedLongGauge(a) {
            @Override
            public Long getNext() {
                return value.get();
            }
        };

        Assert.assertEquals(null, gauge.getValue());

        // increase value and time
        value.set(3);
        Mockito.when(a.currentTime()).thenReturn(2000l);
        Assert.assertEquals(3.0 / 2.0, gauge.getValue(), 0.1);

        // value increase without time increase
        value.set(4);
        Assert.assertEquals(3.0 / 2.0, gauge.getValue(), 0.1);

        // increase time
        Mockito.when(a.currentTime()).thenReturn(3000l);
        Assert.assertEquals(1.0, gauge.getValue(), 0.1);
    }
}
