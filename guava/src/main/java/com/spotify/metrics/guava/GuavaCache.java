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

package com.spotify.metrics.guava;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.MetricIdCache;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public final class GuavaCache<T> implements MetricIdCache.Cache<T> {
    private final MetricIdCache.Loader<T> loader;
    private final Cache<T, MetricId> cache;

    public GuavaCache(MetricIdCache.Loader<T> loader, Cache<T, MetricId> cache) {
        this.loader = loader;
        this.cache = cache;
    }

    @Override
    public MetricId get(final MetricId base, final T key) throws ExecutionException {
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

    public static MetricIdCache.Any setup(final Setup setup) {
        return MetricIdCache.builder().cacheBuilder(new MetricIdCache.CacheBuilder() {
            @Override
            public <T> MetricIdCache.Cache<T> build(final MetricIdCache.Loader<T> loader) {
                final Cache<T, MetricId> cache = setup.setup(CacheBuilder.newBuilder()).build();
                return new GuavaCache<T>(loader, cache);
            }
        });
    }

    public static interface Setup {
        public <K, V> CacheBuilder<K, V> setup(CacheBuilder<K, V> builder);
    }
}
