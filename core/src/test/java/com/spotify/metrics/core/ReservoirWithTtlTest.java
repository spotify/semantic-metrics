package com.spotify.metrics.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.codahale.metrics.Reservoir;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReservoirWithTtlTest {
    @Mock private Reservoir delegateMock;

    private Instant now = Instant.EPOCH;

    private final int ttlSeconds = 10;
    private final int minimumRate = 10;
    private final int bufferSize = ttlSeconds * minimumRate;

    private ReservoirWithTtl reservoir;

    @Before
    public void before() {
        reservoir = new ReservoirWithTtl(delegateMock, ttlSeconds, minimumRate, () -> now);
    }

    @Test
    public void testInternalBufferIsUsedWhenRequestRateIsLow() {
        for (int i = 0; i < bufferSize - 1; i++) {
            reservoir.update(i);
            verify(delegateMock).update(i);
        }

        assertEquals(bufferSize - 1, reservoir.size());
        assertEquals(bufferSize - 1, reservoir.getSnapshot().size());

        verifyNoMoreInteractions(delegateMock);
    }

    @Test
    public void testDelegateIsUsedWhenRequestRateHigh() {
        for (int i = 0; i < bufferSize; i++) {
            reservoir.update(i);
            verify(delegateMock).update(i);
        }

        reservoir.size();
        verify(delegateMock).size();

        reservoir.getSnapshot();
        verify(delegateMock).getSnapshot();

        verifyNoMoreInteractions(delegateMock);
    }

    @Test
    public void testValuesInBufferExpires() {
        for (int i = 0; i < 10; i++) {
            reservoir.update(i);
        }
        assertEquals(10, reservoir.size());

        now = now.plus(1, ChronoUnit.SECONDS);

        for (int i = 0; i < 5; i++) {
            reservoir.update(i);
        }
        assertEquals(15, reservoir.size());

        now = now.plus(9, ChronoUnit.SECONDS);
        assertEquals(15, reservoir.size());
        assertArrayEquals(
            new long[] {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 6, 7, 8, 9},
            reservoir.getSnapshot().getValues());

        now = now.plus(1, ChronoUnit.SECONDS);
        assertEquals(5, reservoir.size());
        assertArrayEquals(new long[] {0, 1, 2, 3, 4}, reservoir.getSnapshot().getValues());
    }
}
