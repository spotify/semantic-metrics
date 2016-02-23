package com.spotify.metrics.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.spotify.metrics.core.MetricIdCache.Loader;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricIdCacheTest {
    @Test
    public void testMutator() {
        final AtomicInteger callCount = new AtomicInteger();

        final MetricIdCache.Loader<String> loader = new MetricIdCache.Loader<String>() {
            @Override
            public MetricId load(MetricId base, String key) {
                callCount.incrementAndGet();
                return base.tagged("key", key);
            }
        };

        final MetricIdCache.Typed<String> builder = MetricIdCache.builder().unbounded(loader);
        final MetricIdCache<String> cache =
            builder.metricId(MetricId.build().tagged("foo", "bar")).build();

        Assert.assertEquals(MetricId.build().tagged("foo", "bar", "key", "test1"),
            cache.get("test1"));
        Assert.assertEquals(MetricId.build().tagged("foo", "bar", "key", "test1"),
            cache.get("test1"));
        Assert.assertEquals(MetricId.build().tagged("foo", "bar", "key", "test2"),
            cache.get("test2"));
        Assert.assertEquals(2, callCount.get());
    }

    @Test
    public void testMissingMetricIdDefaultToEmpty() {
        final MetricIdCache<String> cache =
            MetricIdCache.builder().unbounded(new MetricIdCache.Loader<String>() {
                @Override
                public MetricId load(MetricId id, String key) {
                    return id.tagged("key", key);
                }
            }).build();

        Assert.assertEquals(MetricId.EMPTY.tagged("key", "test"), cache.get("test"));
    }

    @Test(expected = IllegalStateException.class)
    public void testNullMapBuilder() {
        MetricIdCache.builder().mapBuilder(new MetricIdCache.MapBuilder() {
            @Override
            public <T> ConcurrentMap<T, MetricId> build() {
                return null;
            }
        }).metricId(MetricId.build()).loader(new MetricIdCache.Loader<String>() {
            @Override
            public MetricId load(MetricId id, String key) {
                return id.tagged("key", key);
            }
        }).build();
    }

    @Test(expected = IllegalStateException.class)
    public void testNullLoadedValue() {
        MetricIdCache.builder().unbounded(new MetricIdCache.Loader<String>() {
            @Override
            public MetricId load(MetricId id, String key) {
                return null;
            }
        }).metricId(MetricId.build()).build().get("test");
    }

    @Test
    public void testWithGuavaMap() throws InterruptedException {
        MetricIdCache<String> cache =
            MetricIdCache.builder().mapBuilder(new MetricIdCache.MapBuilder() {
                @Override
                public <T> ConcurrentMap<T, MetricId> build() {
                    return CacheBuilder
                        .newBuilder()
                        .expireAfterAccess(1, TimeUnit.SECONDS).<T, MetricId>build().asMap();
                }
            }).metricId(MetricId.build()).loader(new MetricIdCache.Loader<String>() {
                @Override
                public MetricId load(MetricId id, String key) {
                    System.out.println("load");
                    return id.tagged("key", key);
                }
            }).build();

        cache.get("hello");
        cache.get("hello");
        cache.get("hello");
    }

    @Test
    public void testWithGuavaCache() throws InterruptedException {
        MetricIdCache<String> cache =
            MetricIdCache.builder().cacheBuilder(new MetricIdCache.CacheBuilder() {
                @Override
                public <T> MetricIdCache.Cache<T> build(final Loader<T> loader) {
                    final Cache<T, MetricId> cache =
                        CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.SECONDS).build();

                    return new MetricIdCache.Cache<T>() {
                        @Override
                        public MetricId get(final MetricId base, final T key)
                            throws ExecutionException {
                            return cache.get(key, new Callable<MetricId>() {
                                @Override
                                public MetricId call() throws Exception {
                                    return loader.load(base, key);
                                }
                            });
                        }

                        @Override
                        public void invalidate(T key) {
                            cache.invalidate(key);
                        }

                        @Override
                        public void invalidateAll() {
                            cache.invalidateAll();
                        }
                    };
                }
            }).metricId(MetricId.build()).loader(new MetricIdCache.Loader<String>() {
                @Override
                public MetricId load(MetricId id, String key) {
                    return id.tagged("key", key);
                }
            }).build();

        cache.get("hello");
        cache.get("hello");
        cache.get("hello");
    }
}
