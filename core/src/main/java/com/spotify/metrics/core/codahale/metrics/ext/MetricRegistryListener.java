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

package com.spotify.metrics.core.codahale.metrics.ext;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.spotify.metrics.core.DerivingMeter;

public interface MetricRegistryListener extends com.codahale.metrics.MetricRegistryListener {
    /**
     * Called when a {@link DerivingMeter} is added to the registry.
     *
     * @param name          the meter's name
     * @param derivingMeter the meter
     */
    void onDerivingMeterAdded(String name, DerivingMeter derivingMeter);

    /**
     * Called when a {@link DerivingMeter} is removed from the registry.
     *
     * @param name the meter's name
     */
    void onDerivingMeterRemoved(String name);

    /**
     * Called when a distribuition is added to the registry.
     *
     * @param name         the distribution's name
     * @param distribution the distribution
     */
    void onDistributionAdded(String name, Distribution distribution);

    /**
     * Called when a Distribution is removed from the registry.
     *
     * @param name the distribution's name
     */
    void onDistributionRemoved(String name);

    /**
     * A no-op implementation of {@link MetricRegistryListener}.
     */
    abstract class Base implements MetricRegistryListener {
        @Override
        public void onGaugeAdded(String name, Gauge<?> gauge) {
        }

        @Override
        public void onGaugeRemoved(String name) {
        }

        @Override
        public void onCounterAdded(String name, Counter counter) {
        }

        @Override
        public void onCounterRemoved(String name) {
        }

        @Override
        public void onHistogramAdded(String name, Histogram histogram) {
        }

        @Override
        public void onHistogramRemoved(String name) {
        }

        @Override
        public void onMeterAdded(String name, Meter meter) {
        }

        @Override
        public void onMeterRemoved(String name) {
        }

        @Override
        public void onTimerAdded(String name, Timer timer) {
        }

        @Override
        public void onTimerRemoved(String name) {
        }

        @Override
        public void onDerivingMeterAdded(String name, DerivingMeter derivingMeter) {
        }

        @Override
        public void onDerivingMeterRemoved(String name) {
        }

        @Override
        public void onDistributionAdded(String name, Distribution distribution) {

        }

        @Override
        public void onDistributionRemoved(String name) {

        }
    }

}
