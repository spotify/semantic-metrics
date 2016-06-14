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

import com.spotify.metrics.core.*;

import java.util.List;
import java.util.Map;

/**
 * A builder for various remote Metric types.
 *
 * @param <T> Type of Metric to create
 */
public interface SemanticAggregatorMetricBuilder<T extends RemoteMetric> {
    SemanticAggregatorMetricBuilder<RemoteMeter> REMOTE_METERS =
            new SemanticAggregatorMetricBuilder<RemoteMeter>() {
                @Override
                public RemoteMeter newMetric(
                        final MetricId id,
                        final List<String> shardKey,
                        final Remote remote) {

                    final Map<String, String> allAttributes =
                            SemanticAggregator.buildAttributes(id, "meter");
                    final String shard =
                            Sharder.buildShardKey(shardKey, allAttributes);

                    return new RemoteMeter() {
                        @Override
                        public void mark() {
                            mark(1);
                        }

                        @Override
                        public void mark(long n) {
                            remote.post(
                                    "/",
                                    shard,
                                    SemanticAggregator.buildDocument(
                                            Long.toString(n),
                                            id.getKey(),
                                            allAttributes));
                        }

                    };
                }

                @Override
                public boolean isInstance(final RemoteMetric metric) {
                    return RemoteMeter.class.isInstance(metric);
                }
            };

    SemanticAggregatorMetricBuilder<RemoteCounter> REMOTE_COUNTERS =
            new SemanticAggregatorMetricBuilder<RemoteCounter>() {
                @Override
                public RemoteCounter newMetric(
                        final MetricId id,
                        final List<String> shardKey,
                        final Remote remote) {

                    final Map<String, String> allAttributes =
                            SemanticAggregator.buildAttributes(id, "counter");
                    final String shard =
                            Sharder.buildShardKey(shardKey, allAttributes);

                    return new RemoteCounter() {

                        @Override
                        public void inc() {
                            inc(1);
                        }

                        @Override
                        public void inc(long n) {
                            remote.post(
                                    "/",
                                    shard,
                                    SemanticAggregator.buildDocument(
                                            Long.toString(n),
                                            id.getKey(),
                                            allAttributes));
                        }

                        @Override
                        public void dec() {
                            inc(-1);
                        }

                        @Override
                        public void dec(long n) {
                            inc(-n);
                        }

                    };
                }

                @Override
                public boolean isInstance(final RemoteMetric metric) {
                    return RemoteCounter.class.isInstance(metric);
                }
            };

    SemanticAggregatorMetricBuilder<RemoteDerivingMeter> REMOTE_DERIVING_METERS =
            new SemanticAggregatorMetricBuilder<RemoteDerivingMeter>() {
                @Override
                public RemoteDerivingMeter newMetric(
                        final MetricId id,
                        final List<String> shardKey,
                        final Remote remote) {

                    final Map<String, String> allAttributes =
                            SemanticAggregator.buildAttributes(id, "deriving_meter");
                    final String shard =
                            Sharder.buildShardKey(shardKey, allAttributes);

                    return new RemoteDerivingMeter() {
                        @Override
                        public void mark() {
                            mark(1);
                        }

                        @Override
                        public void mark(long n) {
                            remote.post(
                                    "/",
                                    shard,
                                    SemanticAggregator.buildDocument(
                                            Long.toString(n),
                                            id.getKey(),
                                            allAttributes));
                        }

                    };
                }

                @Override
                public boolean isInstance(final RemoteMetric metric) {
                    return RemoteDerivingMeter.class.isInstance(metric);
                }
            };

    T newMetric(MetricId id, List<String> shardKey, Remote remote);

    boolean isInstance(final RemoteMetric metric);
}
