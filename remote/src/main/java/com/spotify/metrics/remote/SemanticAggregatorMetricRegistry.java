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

package com.spotify.metrics.remote;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.spotify.metrics.core.RemoteTimer;
import com.spotify.metrics.core.RemoteHistogram;
import com.spotify.metrics.core.RemoteMeter;
import com.spotify.metrics.core.RemoteDerivingMeter;
import com.spotify.metrics.core.RemoteMetric;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.RemoteSemanticMetricRegistry;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry of remote metric instances. Works just like
 * SemanticMetricRegistry, but only deals with remote metrics.
 */
public class SemanticAggregatorMetricRegistry implements RemoteSemanticMetricRegistry {

    private final Remote remote;
    private final ConcurrentMap<MetricId, RemoteMetric> metrics;

    public SemanticAggregatorMetricRegistry(
        String remoteHost,
        int port,
        int maxConcurrency,
        int hwm) {
        this(new LimitedRemote(new OkRemote(remoteHost, port), maxConcurrency, hwm));
    }

    @VisibleForTesting
    public SemanticAggregatorMetricRegistry(Remote remote) {
        this.remote = remote;
        metrics = new ConcurrentHashMap<>();
    }

    public <T extends RemoteMetric> T getOrAdd(
        final MetricId name,
        final List<String> shardKey,
        final SemanticAggregatorMetricBuilder<T> builder
    ) {
        final RemoteMetric metric = metrics.get(name);

        if (metric != null) {
            if (!builder.isInstance(metric)) {
                throw new IllegalArgumentException(
                    name + " is already used for a different type of metric");
            }

            return (T) metric;
        }

        final T addition = builder.newMetric(name, shardKey, remote);

        final RemoteMetric previous = metrics.putIfAbsent(name, addition);

        if (previous == null) {
            return addition;
        }

        if (!builder.isInstance(previous)) {
            throw new IllegalArgumentException(
                name + " is already used for a different type of metric");
        }

        return (T) previous;
    }

    @Override
    public RemoteTimer timer(MetricId name) {
        return timer(name, ImmutableList.of("what"));
    }

    @Override
    public RemoteTimer timer(MetricId name, List<String> shardKey) {
        return getOrAdd(name, shardKey, SemanticAggregatorMetricBuilder.REMOTE_TIMERS);
    }

    @Override
    public RemoteDerivingMeter derivingMeter(MetricId name, List<String> shardKey) {
        return getOrAdd(name, shardKey, SemanticAggregatorMetricBuilder.REMOTE_DERIVING_METERS);
    }

    @Override
    public RemoteDerivingMeter derivingMeter(MetricId name) {
        return derivingMeter(name, ImmutableList.of("what"));
    }

    @Override
    public RemoteHistogram histogram(final MetricId name) {
        return histogram(name, ImmutableList.of("what"));
    }

    @Override
    public RemoteHistogram histogram(final MetricId name, final List<String> shardKey) {
        return getOrAdd(name, shardKey, SemanticAggregatorMetricBuilder.REMOTE_HISTOGRAM);
    }

    @Override
    public RemoteMeter meter(final MetricId name) {
        return meter(name, ImmutableList.of("what"));
    }

    @Override
    public RemoteMeter meter(final MetricId name, final List<String> shardKey) {
        return getOrAdd(name, shardKey, SemanticAggregatorMetricBuilder.REMOTE_METERS);
    }

}
