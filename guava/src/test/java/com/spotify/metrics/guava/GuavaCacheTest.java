package com.spotify.metrics.guava;

import com.google.common.cache.CacheBuilder;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.MetricIdCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GuavaCacheTest {
    private final MetricIdCache.Any builder = GuavaCache.setup(new GuavaCache.Setup() {
        @Override
        public <K, V> CacheBuilder<K, V> setup(CacheBuilder<K, V> builder) {
            return builder.expireAfterWrite(6, TimeUnit.HOURS);
        }
    });

    private final MetricIdCache.Typed<String> addEndpoint =
        builder.loader(new MetricIdCache.Loader<String>() {
            @Override
            public MetricId load(MetricId id, String endpoint) {
                callCount.incrementAndGet();
                return id.tagged("endpoint", endpoint);
            }
        });

    private AtomicInteger callCount;
    private MetricIdCache<String> endpointCache;

    @Before
    public void setup() {
        callCount = new AtomicInteger();
        endpointCache = addEndpoint.build();
    }

    @Test
    public void testSomeCached() {
        Assert.assertEquals(MetricId.EMPTY.tagged("endpoint", "foo"), endpointCache.get("foo"));
        Assert.assertEquals(MetricId.EMPTY.tagged("endpoint", "foo"), endpointCache.get("foo"));
        Assert.assertEquals(1, callCount.get());

        Assert.assertEquals(MetricId.EMPTY.tagged("endpoint", "bar"), endpointCache.get("bar"));
        Assert.assertEquals(2, callCount.get());
    }
}