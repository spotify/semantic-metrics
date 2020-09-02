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

/*
 * This class was initially copied and modified from the codahale metrics project.
 *
 * For the appropriate license (there was no header) see LICENSE.codahale.txt
 *
 * It was copied from the following tree:
 * https://github.com/dropwizard/metrics/tree/be6989bd082a033c2dd6a57b209f4a67584e3e1a
 */

package com.spotify.metrics.core;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.spotify.metrics.core.codahale.metrics.ext.Distribution;

import java.util.EventListener;

/**
 * Listeners for events from the registry.  Listeners must be thread-safe.
 */
public interface SemanticMetricRegistryListener extends EventListener {
    /**
     * Called when a {@link Gauge} is added to the registry.
     *
     * @param name  the gauge's name
     * @param gauge the gauge
     */
    void onGaugeAdded(MetricId name, Gauge<?> gauge);

    /**
     * Called when a {@link Gauge} is removed from the registry.
     *
     * @param name the gauge's name
     */
    void onGaugeRemoved(MetricId name);

    /**
     * Called when a {@link Counter} is added to the registry.
     *
     * @param name    the counter's name
     * @param counter the counter
     */
    void onCounterAdded(MetricId name, Counter counter);

    /**
     * Called when a {@link Counter} is removed from the registry.
     *
     * @param name the counter's name
     */
    void onCounterRemoved(MetricId name);

    /**
     * Called when a {@link Histogram} is added to the registry.
     *
     * @param name      the histogram's name
     * @param histogram the histogram
     */
    void onHistogramAdded(MetricId name, Histogram histogram);

    /**
     * Called when a {@link Histogram} is removed from the registry.
     *
     * @param name the histogram's name
     */
    void onHistogramRemoved(MetricId name);

    /**
     * Called when a {@link Meter} is added to the registry.
     *
     * @param name  the meter's name
     * @param meter the meter
     */
    void onMeterAdded(MetricId name, Meter meter);

    /**
     * Called when a {@link Meter} is removed from the registry.
     *
     * @param name the meter's name
     */
    void onMeterRemoved(MetricId name);

    /**
     * Called when a {@link Timer} is added to the registry.
     *
     * @param name  the timer's name
     * @param timer the timer
     */
    void onTimerAdded(MetricId name, Timer timer);

    /**
     * Called when a {@link Timer} is removed from the registry.
     *
     * @param name the timer's name
     */
    void onTimerRemoved(MetricId name);

    /**
     * Called when a {@link DerivingMeter} is added to the registry.
     *
     * @param name          the meter's name
     * @param derivingMeter the meter
     */
    void onDerivingMeterAdded(MetricId name, DerivingMeter derivingMeter);

    /**
     * Called when a {@link DerivingMeter} is removed from the registry.
     *
     * @param name the meter's name
     */
    void onDerivingMeterRemoved(MetricId name);

    /**
     * This is a no op implementation for backward compatibility.
     * Please override this method if you are using a Distribution metric.
     * Method is called when a {@link Distribution}  is added to the registry.
     *
     * @param name         the distribution's name
     * @param distribution the distribution
     */
    public default void onDistributionAdded(MetricId name, Distribution distribution) {

    }

    /**
     * This is a no op implementation for backward compatibility.
     * Please override this method if you are using a Distribution metric.
     * Method is called when a {@link Distribution}  is removed from the registry.
     *
     * @param name the distribution's name
     */
    public default void onDistributionRemoved(MetricId name) {

    }

    /**
     * A no-op implementation of {@link SemanticMetricRegistryListener}.
     */
    abstract class Base implements SemanticMetricRegistryListener {
        @Override
        public void onGaugeAdded(MetricId name, Gauge<?> gauge) {
        }

        @Override
        public void onGaugeRemoved(MetricId name) {
        }

        @Override
        public void onCounterAdded(MetricId name, Counter counter) {
        }

        @Override
        public void onCounterRemoved(MetricId name) {
        }

        @Override
        public void onHistogramAdded(MetricId name, Histogram histogram) {
        }

        @Override
        public void onHistogramRemoved(MetricId name) {
        }

        @Override
        public void onMeterAdded(MetricId name, Meter meter) {
        }

        @Override
        public void onMeterRemoved(MetricId name) {
        }

        @Override
        public void onTimerAdded(MetricId name, Timer timer) {
        }

        @Override
        public void onTimerRemoved(MetricId name) {
        }

        @Override
        public void onDerivingMeterAdded(MetricId name, DerivingMeter derivingMeter) {
        }

        @Override
        public void onDerivingMeterRemoved(MetricId name) {
        }

        @Override
        public void onDistributionAdded(MetricId name, Distribution distribution) {

        }

        @Override
        public  void onDistributionRemoved(MetricId name) {

        }
    }

}
