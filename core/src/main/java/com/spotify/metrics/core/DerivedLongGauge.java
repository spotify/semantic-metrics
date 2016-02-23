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

import com.codahale.metrics.Gauge;

import java.util.concurrent.TimeUnit;

public abstract class DerivedLongGauge implements Gauge<Double> {
    private static final SystemTimeProvider DEFAULT_PROVIDER = new SystemTimeProvider();
    private static final long DEFAULT_MINIMAL_TIME_STEP =
        TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

    private final Object lock = new Object();

    private volatile boolean initial = true;
    private volatile Double derived = null;
    private volatile long lastUpdate = 0;
    private volatile Long lastValue = null;

    public static interface TimeProvider {
        long currentTime();
    }

    public static class SystemTimeProvider implements TimeProvider {
        @Override
        public long currentTime() {
            return System.currentTimeMillis();
        }
    }

    private final long minimalTimeStep;
    private final TimeProvider timeProvider;

    private DerivedLongGauge(long minimalTimeStep, TimeProvider timeProvider) {
        this.minimalTimeStep = minimalTimeStep;
        this.timeProvider = timeProvider;
    }

    public DerivedLongGauge() {
        this(DEFAULT_MINIMAL_TIME_STEP, DEFAULT_PROVIDER);
    }

    public DerivedLongGauge(long sourceDuration, TimeUnit sourceUnit) {
        this(sourceDuration, sourceUnit, DEFAULT_PROVIDER);
    }

    /**
     * Only allow access in package to give test-cases a shot.
     *
     * @param timeProvider
     */
    public DerivedLongGauge(TimeProvider timeProvider) {
        this(DEFAULT_MINIMAL_TIME_STEP, timeProvider);
    }

    public DerivedLongGauge(long sourceDuration, TimeUnit sourceUnit, TimeProvider timeProvider) {
        this(TimeUnit.MILLISECONDS.convert(sourceDuration, sourceUnit), timeProvider);
    }

    @Override
    public Double getValue() {
        if (initial) {
            synchronized (lock) {
                if (initial) {
                    this.lastValue = getNext();
                    this.lastUpdate = timeProvider.currentTime();
                    this.initial = false;
                }
            }
        }

        final long currentTime = timeProvider.currentTime();

        if (currentTime - lastUpdate < minimalTimeStep) {
            return this.derived;
        }

        synchronized (lock) {
            if (currentTime - lastUpdate < minimalTimeStep) {
                return this.derived;
            }

            final Double derived = pollAndUpdate(currentTime);
            this.derived = derived;
            this.lastUpdate = currentTime;
            return this.derived;
        }
    }

    private Double pollAndUpdate(final long currentTime) {
        long timeDiff = currentTime - lastUpdate;

        if (timeDiff <= 0) {
            return null;
        }

        final Long currentValue = this.getNext();
        final Long lastValue = this.lastValue;
        this.lastValue = currentValue;

        if (lastValue == null || currentValue == null) {
            return null;
        }

        final double valueDiff = (double) (currentValue - lastValue);

        // ignore negative values.
        if (valueDiff < 0) {
            return null;
        }

        return (valueDiff / timeDiff) * 1000;
    }

    public abstract Long getNext();
}
