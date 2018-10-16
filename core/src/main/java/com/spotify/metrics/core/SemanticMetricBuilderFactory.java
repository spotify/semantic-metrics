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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;
import java.util.function.Supplier;

public class SemanticMetricBuilderFactory {
    public static SemanticMetricBuilder<Timer> timerWithReservoir(
        final Supplier<Reservoir> reservoirSupplier) {
        return new SemanticMetricBuilder<Timer>() {
            @Override
            public Timer newMetric() {
                return new Timer(reservoirSupplier.get());
            }

            @Override
            public boolean isInstance(final Metric metric) {
                return Timer.class.isInstance(metric);
            }
        };
    }

    public static SemanticMetricBuilder<Histogram> histogramWithReservoir(
        final Supplier<Reservoir> reservoirSupplier) {
        return new SemanticMetricBuilder<Histogram>() {
            @Override
            public Histogram newMetric() {
                return new Histogram(reservoirSupplier.get());
            }

            @Override
            public boolean isInstance(final Metric metric) {
                return Histogram.class.isInstance(metric);
            }
        };
    }
}
