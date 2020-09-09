package com.spotify.metrics.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.Metric;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SemanticMetricRegistryTest {
    private static final Map<String, String> tags = ImmutableMap.of("hello", "world");
    private static final Map<String, String> resources = ImmutableMap.of("goodbye", "world");

    @Mock
    private MetricId id;
    @Mock
    private MetricId id2;
    @Mock
    private Metric metric;
    @Mock
    private Metric metric2;
    @Mock
    private ConcurrentMap<MetricId, Metric> metrics;
    @Mock
    private SemanticMetricSet set;

    private SemanticMetricRegistry registry;

    @Before
    public void setUp() {
        registry = spy(new SemanticMetricRegistry(metrics));
    }

    @Test
    public void testRegisterAllEmptyId() {
        doNothing().when(registry).registerAll(MetricId.EMPTY, set);

        registry.registerAll(set);

        verify(registry).registerAll(MetricId.EMPTY, set);
    }

    @Test
    public void testRegisterAll() {
        doReturn("a").when(id2).getKey();
        doReturn(tags).when(id2).getTags();
        doReturn(resources).when(id2).getResources();


        final Map<MetricId, Metric> metricSet = ImmutableMap.of(id2, metric);

        doReturn(metricSet).when(set).getMetrics();
        doReturn(null).when(registry).register(any(MetricId.class), any(Metric.class));

        registry.registerAll(id, set);

        verify(registry).register(new MetricId("a", tags, resources), metric);
    }

    @Test
    public void shouldNotifyOnAdded() {
        final Metric metric = mock(Metric.class);

        doNothing().when(registry).onMetricAdded(id, metric);
        doNothing().when(registry).registerAll(any(SemanticMetricSet.class));

        registry.register(id, metric);

        verify(registry).onMetricAdded(id, metric);
        verify(registry, never()).registerAll(any(SemanticMetricSet.class));
    }

    @Test
    public void shouldNotifyOnRemoved() {
        final Metric metric = mock(Metric.class);

        doNothing().when(registry).onMetricAdded(id, metric);
        doNothing().when(registry).registerAll(any(SemanticMetricSet.class));
        doReturn(metric).when(metrics).remove(id);

        registry.remove(id);

        verify(registry).onMetricRemoved(id, metric);
        verify(registry, never()).registerAll(any(SemanticMetricSet.class));
        verify(metrics).remove(id);
    }

    @Test
    public void shouldGetMetrics() {
        Set<Map.Entry<MetricId, Metric>> entries =
            ImmutableMap.of(id, metric, id2, metric2).entrySet();
        final SemanticMetricFilter filter = mock(SemanticMetricFilter.class);

        doReturn(entries).when(metrics).entrySet();
        doReturn(true).when(filter).matches(id2, metric2);

        final Map<MetricId, Metric> expected = ImmutableMap.of(id2, metric2);

        assertEquals(expected, registry.getMetrics(Metric.class, filter));

        verify(metrics).entrySet();
        verify(filter).matches(id2, metric2);
    }

    @Test
    public void shouldGetDerivingMetrics() {
        final SemanticMetricFilter filter = mock(SemanticMetricFilter.class);
        final SortedMap<MetricId, DerivingMeter> result = mock(SortedMap.class);

        doReturn(result).when(registry).getMetrics(DerivingMeter.class, filter);

        assertEquals(result, registry.getDerivingMeters(filter));

        verify(registry).getMetrics(DerivingMeter.class, filter);
    }

    @Test
    public void shouldGetDistribution() throws Exception {
        final SemanticMetricFilter filter = mock(SemanticMetricFilter.class);
        final SortedMap<MetricId, Distribution> result = mock(SortedMap.class);

        doReturn(result).when(registry).getMetrics(Distribution.class, filter);

        assertEquals(result, registry.getDistributions(filter));

        verify(registry).getMetrics(Distribution.class, filter);
    }

    @Test
    public void testGetOrAddAlreadyExists() throws Exception {
        SemanticMetricBuilder<Metric> builder = mock(SemanticMetricBuilder.class);
        doReturn(metric).when(metrics).get(id);
        doReturn(true).when(builder).isInstance(metric);

        assertEquals(metric, registry.getOrAdd(id, builder));

        verify(builder, times(1)).isInstance(metric);
        verify(registry, never()).addIfAbsent(id, metric);
    }

    @Test
    public void testGetOrAddNew() {
        SemanticMetricBuilder<Metric> builder = mock(SemanticMetricBuilder.class);
        doReturn(null).when(metrics).get(id);

        // TODO: this is a broken branch, when even check type?
        doReturn(metric).when(builder).newMetric();
        doReturn(metric).when(registry).register(id, metric);

        assertEquals(metric, registry.getOrAdd(id, builder));

        verify(builder, never()).isInstance(metric);
        verify(registry, times(1)).addIfAbsent(id, metric);
    }

    @Test
    public void testAddIfAbsentMissing() {
        doReturn(null).when(metrics).putIfAbsent(id, metric);
        doNothing().when(registry).onMetricAdded(id, metric);

        assertNull(registry.addIfAbsent(id, metric));

        verify(registry).onMetricAdded(id, metric);
        verify(metrics).putIfAbsent(id, metric);
    }

    @Test
    public void testAddIfAbsentExists() {
        doReturn(metric2).when(metrics).putIfAbsent(id, metric);
        doNothing().when(registry).onMetricAdded(id, metric);

        assertEquals(metric2, registry.addIfAbsent(id, metric));

        verify(registry, never()).onMetricAdded(id, metric);
        verify(metrics).putIfAbsent(id, metric);
    }
}
