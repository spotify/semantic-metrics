/*
 * Copyright (c) 2018 Spotify AB.
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

import static java.util.stream.Collectors.toList;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.google.common.collect.EvictingQueue;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ReservoirWithTtl implements Reservoir {
    private class ValueAndTimestamp {
        public long value;
        public Instant timestamp;

        public ValueAndTimestamp(final long value, final Instant timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    private static final int DEFAULT_TTL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(5);

    private static final int DEFAULT_MINIMUM_RATE = 10;

    private static Constructor snapshotConstructor;

    private final int ttlSeconds;

    private final int bufferSize;

    private final Reservoir delegate;

    private final EvictingQueue<ValueAndTimestamp> valueBuffer;

    private final Supplier<Instant> now;

    static {
        // There is a breaking API change between metrics-core 3.1 and 3.2 where
        // com.codahale.metrics.Snapshot becomes abstract and is replaced by UniformSnapshot.
        // There are more breaking changes between the two versions (bumbing breaks hermes-java for
        // instance). On the other hand, newer versions of the datastax drivers use the newer
        // version. This means that to not force anyone to make a lot of changes to be able to use
        // this library, we need to support both versions. The below code is the most sane way we
        // have found to do that: We find one class out of the two candidates that is possible to
        // construct and save a reference to its constructor.
        try {
            snapshotConstructor =
                Class.forName("com.codahale.metrics.Snapshot").getConstructor(Collection.class);
        } catch (final ClassNotFoundException | NoSuchMethodException e) {
            try {
                snapshotConstructor = Class
                    .forName("com.codahale.metrics.UniformSnapshot")
                    .getConstructor(Collection.class);
            } catch (final ClassNotFoundException | NoSuchMethodException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public ReservoirWithTtl() {
        this(new ExponentiallyDecayingReservoir(), DEFAULT_TTL_SECONDS, DEFAULT_MINIMUM_RATE);
    }

    public ReservoirWithTtl(final int ttlSeconds) {
        this(new ExponentiallyDecayingReservoir(), ttlSeconds, DEFAULT_MINIMUM_RATE);
    }

    public ReservoirWithTtl(final Reservoir delegate, final int ttlSeconds, final int minimumRate) {
        this(delegate, ttlSeconds, minimumRate, Instant::now);
    }

    public ReservoirWithTtl(
        final Reservoir delegate,
        final int ttlSeconds,
        final int minimumRate,
        final Supplier<Instant> now) {
        this.delegate = delegate;
        this.now = now;
        this.ttlSeconds = ttlSeconds;
        this.bufferSize = ttlSeconds * minimumRate;
        this.valueBuffer = EvictingQueue.create(bufferSize);
    }

    @Override
    public int size() {
        synchronized (this) {
            purgeOld();
            if (useInternalBuffer()) {
                return valueBuffer.size();
            }
        }

        return delegate.size();
    }

    @Override
    public void update(final long value) {
        synchronized (this) {
            valueBuffer.add(new ValueAndTimestamp(value, now.get()));
        }

        delegate.update(value);
    }

    @Override
    public Snapshot getSnapshot() {
        synchronized (this) {
            purgeOld();
            if (useInternalBuffer()) {
                return getInternalSnapshot();
            }
        }

        return delegate.getSnapshot();
    }

    private boolean useInternalBuffer() {
        return valueBuffer.size() < bufferSize;
    }

    private void purgeOld() {
        final Instant cutoffTime = now.get().minusSeconds(ttlSeconds);
        while (!valueBuffer.isEmpty() && valueBuffer.peek().timestamp.isBefore(cutoffTime)) {
            valueBuffer.remove();
        }
    }

    private Snapshot getInternalSnapshot() {
        try {
            // See comment at static initializer why we need to use constructor reference
            return (Snapshot) snapshotConstructor.newInstance(
                valueBuffer.stream().map(v -> v.value).collect(toList()));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
