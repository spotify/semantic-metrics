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

import com.codahale.metrics.Meter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of a DerivingMeter using a {@link com.codahale.metrics.Meter} for all the hard
 * work.
 */
class DelegatingDerivingMeter implements DerivingMeter {
    private final Meter delegate;
    private final AtomicLong lastValue = new AtomicLong(-1);

    public DelegatingDerivingMeter(Meter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void mark(long currentValue) {
        if (currentValue < 0) {
            throw new IllegalArgumentException("Negative values not allowed, got: " + currentValue);
        }

        long previous = lastValue.getAndSet(currentValue);

        if (previous < 0) {
            // discard initial value
            return;
        }

        long delta = currentValue - previous;

        if (delta < 0) {
            // discard negative values; the rationale is that this should only happen if the
            // outer value is reset,
            // for instance due to a restart or something. Reporting a (potentially very) large
            // negative value at that
            // point would lead to strange gaps. The other way you can get a negative value is if
            // updates occur out of
            // order, and this is expected to be extremely rare, so there's no need to deal with
            // that special case.
            return;
        }

        delegate.mark(delta);
    }

    @Override
    public long getCount() {
        return delegate.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return delegate.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return delegate.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return delegate.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return delegate.getOneMinuteRate();
    }
}
