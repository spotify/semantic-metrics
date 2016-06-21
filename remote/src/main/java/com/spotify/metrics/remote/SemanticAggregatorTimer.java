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

import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.RemoteTimer;

import java.util.List;
import java.util.Map;

/**
 * Remote implementation of a codahale Timer.
 */
public class SemanticAggregatorTimer implements RemoteTimer {

    private static final TimeSource defaultTimeSource = new TimeSource() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    };
    final String key;
    final Remote remote;
    final Map<String, String> allAttributes;
    final String shard;
    final TimeSource timeSource;

    public SemanticAggregatorTimer(
        final MetricId id,
        final List<String> shardKey,
        final Remote remote,
        final TimeSource timeSource) {
        this.key = id.getKey();
        this.remote = remote;
        this.timeSource = timeSource;
        allAttributes = SemanticAggregator.buildAttributes(id, "timer");
        shard = Sharder.buildShardKey(shardKey, allAttributes);
    }

    public SemanticAggregatorTimer(MetricId id, List<String> shardKey, Remote remote) {
        this(id, shardKey, remote, defaultTimeSource);
    }

    @Override
    public RemoteTimer.Context time() {
        final long startTm = timeSource.nanoTime();
        return new RemoteTimer.Context() {
            @Override
            public void stop() {
                long stopTm = timeSource.nanoTime();
                remote.post(
                    "/",
                    shard,
                    SemanticAggregator.buildDocument(
                        Long.toString(stopTm - startTm),
                        key,
                        allAttributes));
            }
        };
    }

    interface TimeSource {
        long nanoTime();
    }
}
