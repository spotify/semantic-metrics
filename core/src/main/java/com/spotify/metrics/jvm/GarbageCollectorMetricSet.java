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

package com.spotify.metrics.jvm;

import com.codahale.metrics.Metric;
import com.spotify.metrics.core.DerivedLongGauge;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricSet;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of gauges for the counts and elapsed times of garbage collections.
 */
public class GarbageCollectorMetricSet implements SemanticMetricSet {
    private final List<GarbageCollectorMXBean> garbageCollectors;

    /**
     * Creates a new set of gauges for all discoverable garbage collectors.
     */
    public GarbageCollectorMetricSet() {
        this(ManagementFactory.getGarbageCollectorMXBeans());
    }

    /**
     * Creates a new set of gauges for the given collection of garbage collectors.
     *
     * @param garbageCollectors the garbage collectors
     */
    public GarbageCollectorMetricSet(
        Collection<GarbageCollectorMXBean> garbageCollectors
    ) {
        this.garbageCollectors = new ArrayList<GarbageCollectorMXBean>(garbageCollectors);
    }

    @Override
    public Map<MetricId, Metric> getMetrics() {
        final Map<MetricId, Metric> gauges = new HashMap<MetricId, Metric>();
        final MetricId base = MetricId.build();

        for (final GarbageCollectorMXBean m : garbageCollectors) {
            final MetricId gc = base.tagged("gc", m.getName());

            final MetricId collectionCount =
                gc.tagged("what", "jvm-gc-collections", "unit", "collection/s");
            final MetricId collectionTime =
                gc.tagged("what", "jvm-gc-collection-time", "unit", "ms/s");

            gauges.put(collectionCount, new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return m.getCollectionCount();
                }
            });

            gauges.put(collectionTime, new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return m.getCollectionTime();
                }
            });
        }

        return Collections.unmodifiableMap(gauges);
    }
}
