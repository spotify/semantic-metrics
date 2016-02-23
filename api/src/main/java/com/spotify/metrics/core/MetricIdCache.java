/*
 * Copyright (c) 2016 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.metrics.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

// @formatter:off
/**
 * <p>A utility class that simplifies mutating and storing a MetricId.</p>
 *
 * <h1>Example</h1>
 *
 * <code><pre>
 * // example using Guava.
 * final MetricIdCache.CacheBuilder cacheBuilder = new MetricIdCache.CacheBuilder() {
 *     public <T> ConcurrentMap<T, MetricId> build() {
 *         return CacheBuilder.newBuilder()
 *             .expireAfterWrite(12, TimeUnit.HOURS).build().asMap()
 *     }
 * };
 *
 * final MetricIdCache.Builder<String> builder = MetricIdCache.builder().cacheBuilder(cacheBuilder);
 *
 * final MetricIdCache<String> cache = builder.metricId(MetricId.build().tagged("foo", "bar"))
 * .loader(new MetricIdCache.Loader<String>() {
 *     public MetricId load(MetricId base, String key) {
 *         base.tagged("key", key);
 *     }
 * }).build();
 *
 * cache.get("hello"); // MetricId(foo=bar, key=hello)
 * cache.get("world"); // MetricId(foo=bar, key=world)
 *
 * // subsequent calls with cache.get("hello"), or cache.get("world") will return the same
 * MetricId instance.
 * </pre></code>
 *
 * @author udoprog
 * @param <T> The key type for loading the metric id.
 */
// @formatter:on
public class MetricIdCache<T> {
    private final Cache<T> cache;
    private final MetricId metricId;

    private MetricIdCache(Cache<T> cache, MetricId metricId) {
        this.cache = cache;
        this.metricId = metricId;
    }

    public static Any builder() {
        return new Any();
    }

    /**
     * Get the value for the specified key.
     * <p>
     * This method is guaranteed to return the same instance of the associated MetricId, regardless
     * of how many threads invoke it.
     * <p>
     * The configured Loader could be called multiple times, and should be idempotent to that.
     *
     * @param key The given key to get MetricId for.
     * @return A metric id for the given key.
     * @throws IllegalStateException if loader returns {@code null}.
     */
    public MetricId get(T key) {
        try {
            return cache.get(metricId, key);
        } catch (ExecutionException e) {
            throw new RuntimeException("failed to get cache value", e);
        }
    }

    public void invalidate(T key) {
        cache.invalidate(key);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public static interface Loader<T> {
        public MetricId load(MetricId id, T key);
    }

    public static interface Cache<T> {
        public MetricId get(MetricId metricId, T key) throws ExecutionException;

        public void invalidate(T key);

        public void invalidateAll();
    }

    public static interface MapBuilder {
        public <T> ConcurrentMap<T, MetricId> build();
    }

    public static interface CacheBuilder {
        public <T> Cache<T> build(Loader<T> loader);
    }

    private static final class ConcurrentMapCache<T> implements Cache<T> {
        private final ConcurrentMap<T, MetricId> cache;
        private final Loader<T> loader;

        private ConcurrentMapCache(ConcurrentMap<T, MetricId> cache, Loader<T> loader) {
            this.cache = cache;
            this.loader = loader;
        }

        @Override
        public MetricId get(MetricId metricId, T key) {
            final MetricId candidate = cache.get(key);

            if (candidate != null) {
                return candidate;
            }

            final MetricId addition = loader.load(metricId, key);

            if (addition == null) {
                throw new IllegalStateException("loader returned null value");
            }

            final MetricId put;

            if ((put = cache.putIfAbsent(key, addition)) != null) {
                return put;
            }

            return addition;
        }

        @Override
        public void invalidate(T key) {
            cache.remove(key);
        }

        @Override
        public void invalidateAll() {
            cache.clear();
        }
    }

    private interface TypedCacheBuilder<T> {
        Cache<T> build(Loader<T> loader);
    }

    /**
     * A builder for which the metric id cache has a given type.
     *
     * @param <T> The type of the metric id cache.
     */
    public static class Typed<T> {
        private final Any any;
        private final Cache<T> cache;
        private final Loader<T> loader;

        /* internal */
        private final TypedCacheBuilder<T> typedCacheBuilder;

        private Typed(
            Any any, Cache<T> cache, Loader<T> loader, TypedCacheBuilder<T> typedCacheBuilder
        ) {
            this.any = any;
            this.cache = cache;
            this.loader = loader;
            this.typedCacheBuilder = typedCacheBuilder;
        }

        /**
         * Configure a base metric id.
         *
         * @see Any#metricId(MetricId)
         */
        public Typed<T> metricId(MetricId base) {
            return any(any.metricId(base));
        }

        /**
         * Use a shared cache.
         */
        public Typed<T> cache(Cache<T> cache) {
            if (cache == null) {
                throw new IllegalArgumentException("'cache' must not be null");
            }

            return new Typed<T>(any, cache, loader, typedCacheBuilder);
        }

        /**
         * Allows an unbounded cache.
         * <p>
         * This could leak memory, since the provided cache implementation will never expire
         * entries.
         *
         * @see Any#unbounded(Loader)
         */
        public Typed<T> unbounded(Loader<T> loader) {
            return cache(new ConcurrentHashMap<T, MetricId>(), loader);
        }

        /**
         * Configure a specific cache instance, use this if you wish for the consequent builders to
         * re-use the same cache instance.
         */
        public Typed<T> cache(final ConcurrentMap<T, MetricId> map, Loader<T> loader) {
            if (map == null) {
                throw new IllegalArgumentException("'cache' must not be null");
            }

            return loader(loader).typedCacheBuilder(new TypedCacheBuilder<T>() {
                @Override
                public Cache<T> build(Loader<T> loader) {
                    return new ConcurrentMapCache<T>(map, loader);
                }
            });
        }

        public Typed<T> cacheBuilder(CacheBuilder cacheBuilder) {
            return any(any.cacheBuilder(cacheBuilder));
        }

        public Typed<T> mapBuilder(MapBuilder mapBuilder, Loader<T> loader) {
            return loader(loader).any(any.mapBuilder(mapBuilder));
        }

        public Typed<T> mapBuilder(MapBuilder mapBuilder) {
            return any(any.mapBuilder(mapBuilder));
        }

        public Typed<T> loader(Loader<T> loader) {
            if (cache != null) {
                throw new IllegalStateException("shared cache is already set");
            }

            return new Typed<T>(any, cache, loader, typedCacheBuilder);
        }

        /**
         * Build a cache with the given MetricId base.
         */
        public MetricIdCache<T> build() {
            final Cache<T> cache = any.buildCache(this.cache, this.loader, this.typedCacheBuilder);
            return new MetricIdCache<T>(cache, any.buildMetricId());
        }

        private Typed<T> typedCacheBuilder(TypedCacheBuilder<T> typedCacheBuilder) {
            return new Typed<T>(any, cache, loader, typedCacheBuilder);
        }

        private Typed<T> any(Any any) {
            return new Typed<>(any, cache, loader, typedCacheBuilder);
        }
    }

    /**
     * A metric id cache builder that can take any type.
     * <p>
     * Calling one of the type-specific configuration options will returned a regular, typed
     * Builder.
     *
     * @author udoprog
     */
    public static class Any {
        private final MetricId metricId;
        private final CacheBuilder cacheBuilder;

        private Any(MetricId base, CacheBuilder cacheBuilder) {
            this.metricId = base;
            this.cacheBuilder = cacheBuilder;
        }

        private Any() {
            this(null, null);
        }

        /**
         * Set the given base metric id for the cache.
         */
        public Any metricId(MetricId metricId) {
            if (metricId == null) {
                throw new IllegalArgumentException("'metricId' must not be null");
            }

            return new Any(metricId, cacheBuilder);
        }

        public <T> Typed<T> unbounded(Loader<T> loader) {
            return new Typed<T>(this, null, null, null).unbounded(loader);
        }

        public <T> Typed<T> cache(Cache<T> cache) {
            return new Typed<T>(this, null, null, null).cache(cache);
        }

        public <T> Typed<T> cache(ConcurrentMap<T, MetricId> cache, Loader<T> loader) {
            return new Typed<T>(this, null, null, null).cache(cache, loader);
        }

        public <T> Typed<T> loader(Loader<T> loader) {
            return new Typed<T>(this, null, null, null).loader(loader);
        }

        public <T> Typed<T> mapBuilder(MapBuilder mapBuilder, Loader<T> loader) {
            return mapBuilder(mapBuilder).loader(loader);
        }

        public Any cacheBuilder(CacheBuilder cacheBuilder) {
            if (cacheBuilder == null) {
                throw new IllegalArgumentException("'cacheBuilder' must not be null");
            }

            return new Any(metricId, cacheBuilder);
        }

        public Any mapBuilder(final MapBuilder mapBuilder) {
            if (mapBuilder == null) {
                throw new IllegalArgumentException("'mapBuilder' must not be null");
            }

            return new Any(metricId, new CacheBuilder() {
                @Override
                public <T> Cache<T> build(Loader<T> loader) {
                    final ConcurrentMap<T, MetricId> map = mapBuilder.build();

                    if (map == null) {
                        throw new IllegalStateException("'mapBuilder' must not return null");
                    }

                    return new ConcurrentMapCache<T>(map, loader);
                }
            });
        }

        private <T> Cache<T> buildCache(
            Cache<T> cache, Loader<T> loader, TypedCacheBuilder<T> typedCacheBuilder
        ) {
            if (cache != null) {
                return cache;
            }

            if (loader == null) {
                throw new IllegalStateException("'loader' must be set");
            }

            if (cacheBuilder != null) {
                final Cache<T> c = cacheBuilder.build(loader);

                if (c == null) {
                    throw new IllegalStateException("'cacheBuilder' must return non-null values");
                }

                return c;
            }

            if (typedCacheBuilder != null) {
                return typedCacheBuilder.build(loader);
            }

            throw new IllegalStateException("No cache implementation is configured");
        }

        private MetricId buildMetricId() {
            if (metricId == null) {
                return MetricId.EMPTY;
            }

            return metricId;
        }
    }
}
