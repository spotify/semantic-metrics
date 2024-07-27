/*
 * Copyright (C) 2021 Spotify AB.
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

import com.google.common.annotations.VisibleForTesting;
import com.tdunning.math.stats.TDigest;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class ConcurrentDistribution implements Distribution {

    private final Distribution[] shards;
    private final int shardBitmask;

    @VisibleForTesting
    public ConcurrentDistribution() {
        this(SemanticMetricDistribution::new);
    }

    ConcurrentDistribution(Supplier<Distribution> distributionSupplier) {
        this(distributionSupplier, 4 * Runtime.getRuntime().availableProcessors());
    }

    ConcurrentDistribution(Supplier<Distribution> distributionSupplier, int minShards) {
        final int numShards = nearestPowerOfTwo(minShards);
        this.shardBitmask = numShards - 1;

        this.shards = IntStream.range(0, numShards)
                .mapToObj(i -> distributionSupplier.get())
                .toArray(Distribution[]::new);
    }

    private static int nearestPowerOfTwo(int n) {
        int x = 1;
        while (x < n) {
            x *= 2;
        }
        return x;
    }

    @Override
    public void record(double val) {
        final int targetShard = ((int) Thread.currentThread().getId() & shardBitmask);
        shards[targetShard].record(val);
    }

    @Override
    public TDigest getDigestAndFlush() {
        return Arrays.stream(shards)
                .map(Distribution::getDigestAndFlush)
                .reduce((first, second) -> {
                    first.add(second);
                    return first;
                })
                .get();
    }

    @Override
    public long getCount() {
        return Arrays.stream(shards).mapToLong(Distribution::getCount).sum();
    }
}
