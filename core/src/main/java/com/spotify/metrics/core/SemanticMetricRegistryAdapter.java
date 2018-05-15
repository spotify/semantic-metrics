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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener to {@link MetricRegistry} addition/removal events, that mirrors
 * them into a {@link SemanticMetricRegistry}.
 */
public class SemanticMetricRegistryAdapter implements MetricRegistryListener {

    private static final Logger log = LoggerFactory.getLogger(SemanticMetricRegistryAdapter.class);
    private final SemanticMetricRegistry target;
    private final SemanticMetricIdAdapter metricIdBuilder;

    public static MetricRegistry adaptingMetricRegistry(
        SemanticMetricRegistry target, SemanticMetricIdAdapter metricIdBuilder
    ) {
        MetricRegistry metricRegistry = new MetricRegistry();
        metricRegistry.addListener(new SemanticMetricRegistryAdapter(target, metricIdBuilder));
        return metricRegistry;
    }

    public SemanticMetricRegistryAdapter(
        SemanticMetricRegistry target, SemanticMetricIdAdapter metricIdBuilder
    ) {
        this.target = target;
        this.metricIdBuilder = metricIdBuilder;
    }

    @Override
    public void onGaugeAdded(String name, Gauge<?> gauge) {
        tryRegister(name, gauge);
    }

    @Override
    public void onGaugeRemoved(String name) {
        tryRemove(name);
    }

    @Override
    public void onCounterAdded(String name, Counter counter) {
        tryRegister(name, counter);
    }

    @Override
    public void onCounterRemoved(String name) {
        tryRemove(name);
    }

    @Override
    public void onHistogramAdded(String name, Histogram histogram) {
        tryRegister(name, histogram);
    }

    @Override
    public void onHistogramRemoved(String name) {
        tryRemove(name);
    }

    @Override
    public void onMeterAdded(String name, Meter meter) {
        tryRegister(name, meter);
    }

    @Override
    public void onMeterRemoved(String name) {
        tryRemove(name);
    }

    @Override
    public void onTimerAdded(String name, Timer timer) {
        tryRegister(name, timer);
    }

    @Override
    public void onTimerRemoved(String name) {
        tryRemove(name);
    }

    private <T extends Metric> void tryRegister(String name, T metric) {
        try {
            MetricId metricId = metricIdBuilder.buildMetricId(name);
            target.register(metricId, metric);
        } catch (Exception e) {
            log.warn("Failed to register metric " + name, e);
        }
    }

    private void tryRemove(String name) {
        try {
            MetricId metricId = metricIdBuilder.buildMetricId(name);
            target.remove(metricId);
        } catch (Exception e) {
            log.warn("failed to remove metric " + name, e);
        }
    }
}
