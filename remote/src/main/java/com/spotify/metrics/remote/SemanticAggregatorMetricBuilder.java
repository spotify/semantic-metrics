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

import com.google.common.collect.ImmutableMap;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.RemoteMeter;
import com.spotify.metrics.core.RemoteMetric;

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

                    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
                    builder.putAll(id.getTags());
                    builder.put("metric_type", "meter");

                    final Map<String, String> allAttributes = builder.build();

                    // Fixme(liljencrantz): This logic should live in separate class
                    StringBuilder shardBuilder = new StringBuilder();
                    for (String key : shardKey) {
                        if (shardBuilder.length() != 0) {
                            shardBuilder.append(',');
                        }
                        shardBuilder.append(httpHeaderEscape(key));
                        shardBuilder.append(':');
                        shardBuilder.append(httpHeaderEscape(allAttributes.get(key)));
                    }
                    final String shard = shardBuilder.toString();

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
                                    ImmutableMap.of(
                                            "type", "metric",
                                            "value", Long.toString(n),
                                            "key", id.getKey(),
                                            "attributes", allAttributes));
                        }

                    };
                }

                // Fixme(liljencrantz): This logic should live in separate class
                private String httpHeaderEscape(String key) {
                    StringBuilder res = new StringBuilder();
                    for (char c : key.toCharArray()) {
                        if (((c >= 'a' && c <= 'z')) || ((c >= 'A' && c <= 'Z'))) {
                            res.append(c);
                        }
                    }
                    return res.toString();
                }

                @Override
                public boolean isInstance(final RemoteMetric metric) {
                    return RemoteMeter.class.isInstance(metric);
                }
            };

    T newMetric(MetricId id, List<String> shardKey, Remote remote);

    boolean isInstance(final RemoteMetric metric);
}
