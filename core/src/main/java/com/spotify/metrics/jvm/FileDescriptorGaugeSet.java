/*
 * Copyright (c) 2017 Spotify AB.
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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of gauges for the ratio of allocated file descriptors.
 */
public class FileDescriptorGaugeSet implements SemanticMetricSet {

    private FileDescriptorRatioGauge fileDescriptorRatioGauge;

    public FileDescriptorGaugeSet() {
        fileDescriptorRatioGauge = new FileDescriptorRatioGauge();
    }

    @Override
    public Map<MetricId, Metric> getMetrics() {
        final Map<MetricId, Metric> gauges = new HashMap<MetricId, Metric>();
        final MetricId metricId =
                MetricId.build().tagged("what", "file-descriptor-ratio", "unit", "%");

        gauges.put(metricId, new Gauge<Object>() {
            @Override
            public Object getValue() {
                    // Java 9 will throw java.lang.reflect.InaccessibleObjectException
                    // which does not exist in Java 8, therefore return 0.
                    try {
                        return fileDescriptorRatioGauge.getValue();
                    } catch (Exception e) {
                        return 0;
                    }
            }
        });
        return Collections.unmodifiableMap(gauges);
    }
}
