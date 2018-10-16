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

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;

/**
 * A quick and easy way of capturing the notion of default metrics.
 */
public interface SemanticMetricBuilder<T extends Metric> {
    SemanticMetricBuilder<Counter> COUNTERS = new SemanticMetricBuilder<Counter>() {
        @Override
        public Counter newMetric() {
            return new Counter();
        }

        @Override
        public boolean isInstance(final Metric metric) {
            return Counter.class.isInstance(metric);
        }
    };

    SemanticMetricBuilder<Histogram> HISTOGRAMS = SemanticMetricBuilderFactory
        .histogramWithReservoir(() -> new ExponentiallyDecayingReservoir());

    SemanticMetricBuilder<Meter> METERS = new SemanticMetricBuilder<Meter>() {
        @Override
        public Meter newMetric() {
            return new Meter();
        }

        @Override
        public boolean isInstance(final Metric metric) {
            return Meter.class.isInstance(metric);
        }
    };

    SemanticMetricBuilder<Timer> TIMERS = SemanticMetricBuilderFactory
        .timerWithReservoir(() -> new ExponentiallyDecayingReservoir());

    SemanticMetricBuilder<DerivingMeter> DERIVING_METERS =
        new SemanticMetricBuilder<DerivingMeter>() {
            @Override
            public DelegatingDerivingMeter newMetric() {
                return new DelegatingDerivingMeter(new Meter());
            }

            @Override
            public boolean isInstance(Metric metric) {
                return DerivingMeter.class.isInstance(metric);
            }
        };

    T newMetric();

    boolean isInstance(final Metric metric);
}
