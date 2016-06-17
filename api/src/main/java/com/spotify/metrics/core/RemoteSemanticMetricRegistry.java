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

import java.util.List;

/**
 * Interface for arbitrary implementation of a MetricRegistry that
 * does not need to be running locally in the current process. This
 * API leaks less implementation details about how the metrics are
 * implemented under the hood.
 * <p>
 * Currently, the SemanticAggregatorMetricRegistry is the only implementation.
 */
public interface RemoteSemanticMetricRegistry {
    /**
     * Creates a new {@link RemoteTimer} and registers it under the given name.
     * Sharding uses the "what"-tag of the metric Id.
     *
     * @param name the name of the metric
     * @return a new {@link RemoteTimer}
     */
    RemoteTimer timer(final MetricId name);

    /**
     * Creates a new {@link RemoteTimer} and registers it under the given name.
     * Sharding uses the "what"-tag of the metric Id.
     *
     * @param name the name of the metric
     * @param shardKey the list of tags to be used for sharding
     * @return a new {@link RemoteTimer}
     */
    RemoteTimer timer(final MetricId name, final List<String> shardKey);

    /**
     * Creates a new {@link RemoteMeter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @param shardKey the list of tags to be used for sharding
     * @return a new {@link RemoteMeter}
     */
    RemoteDerivingMeter derivingMeter(final MetricId name, final List<String> shardKey);

    /**
     * Creates a new {@link RemoteMeter} and registers it under the given name.
     * Sharding uses the "what"-tag of the metric Id.
     *
     * @param name the name of the metric
     * @return a new {@link RemoteMeter}
     */
    RemoteDerivingMeter derivingMeter(final MetricId name);

    /**
     * Creates a new {@link RemoteMeter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @param shardKey the list of tags to be used for sharding
     * @return a new {@link RemoteMeter}
     */
    RemoteHistogram histogram(final MetricId name, final List<String> shardKey);

    /**
     * Creates a new {@link RemoteMeter} and registers it under the given name.
     * Sharding uses the "what"-tag of the metric Id.
     *
     * @param name the name of the metric
     * @return a new {@link RemoteMeter}
     */
    RemoteHistogram histogram(final MetricId name);

    /**
     * Creates a new {@link RemoteMeter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @param shardKey the list of tags to be used for sharding
     * @return a new {@link RemoteMeter}
     */
    RemoteMeter meter(final MetricId name, final List<String> shardKey);

    /**
     * Creates a new {@link RemoteMeter} and registers it under the given name.
     * Sharding uses the "what"-tag of the metric Id.
     *
     * @param name the name of the metric
     * @return a new {@link RemoteMeter}
     */
    RemoteMeter meter(final MetricId name);
}
