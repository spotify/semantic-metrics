package com.spotify.metrics.remote;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.RemoteMeter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * This test tests the SemanticAggregatorMetricRegistry and RemoteSementicMetricBuilder
 * classes.
 */
@RunWith(MockitoJUnitRunner.class)
public class SemanticAggregatorMetricRegistryTest {

    @Mock
    Remote remote;

    SemanticAggregatorMetricRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = spy(new SemanticAggregatorMetricRegistry(remote));
    }

    @Test
    public void createMeterTest() {
        RemoteMeter meter = registry.meter(MetricId.build("X").tagged("what", "balls"));
        assertNotNull(meter);
    }

    @Test
    public void meterBump1Test() {
        registry.meter(MetricId.build("X").tagged("what", "balls", "status", "tripping")).mark();
        verify(remote).post("/", "what:balls",
                ImmutableMap.of(
                        "type", "metric",
                        "value", "1",
                        "key", "X",
                        "attributes", ImmutableMap.of(
                                "metric_type", "meter",
                                "status", "tripping",
                                "what", "balls")));
    }

    @Test
    public void meterBump5Test() {
        registry.meter(MetricId.build("X").tagged("what", "balls", "status", "tripping")).mark(5);
        verify(remote).post("/", "what:balls",
                ImmutableMap.of(
                        "type", "metric",
                        "value", "5",
                        "key", "X",
                        "attributes", ImmutableMap.of(
                                "metric_type", "meter",
                                "status", "tripping",
                                "what", "balls")));
    }

    @Test
    public void meterShardTest() {
        registry.meter(
                MetricId.build("X").tagged("what", "balls", "status", "tripping"),
                ImmutableList.of("what", "status")).mark();
        verify(remote).post(anyString(), eq("what:balls,status:tripping"), anyMap());
    }

    @Test
    public void uniquenessTest() {
        RemoteMeter a = registry.meter(MetricId.build("X").tagged("what", "balls", "status", "tripping"));
        RemoteMeter b = registry.meter(MetricId.build("X").tagged("what", "balls", "status", "tripping"));
        assert (a == b);
    }
}
