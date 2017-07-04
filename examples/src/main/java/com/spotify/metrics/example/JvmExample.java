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

package com.spotify.metrics.example;

import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwd.FastForwardReporter;
import com.spotify.metrics.jvm.CpuGaugeSet;
import com.spotify.metrics.jvm.FileDescriptorGaugeSet;
import com.spotify.metrics.jvm.GarbageCollectorMetricSet;
import com.spotify.metrics.jvm.MemoryUsageGaugeSet;
import com.spotify.metrics.jvm.ThreadStatesMetricSet;

import java.util.concurrent.TimeUnit;

/**
 * An example application that collects JVM statistics and reports them into FastForward.
 * <p>
 * To run this example and see the output please setup a local ffwd and run ffwd debugger. Follow
 * this link to see the https://github.com/spotify/ffwd#local-debugging
 */
public class JvmExample {
    private static final MetricId APP_PREFIX = MetricId.build("jvm-example");
    private static final SemanticMetricRegistry registry = new SemanticMetricRegistry();

    public static void main(final String[] args) throws Exception {
        registry.register(MetricId.build("jvm-memory"), new MemoryUsageGaugeSet());
        registry.register(MetricId.build("jvm-gc"), new GarbageCollectorMetricSet());
        registry.register(MetricId.build("jvm-threads"), new ThreadStatesMetricSet());
        registry.register(MetricId.build("jvm-cpu"), CpuGaugeSet.create());
        registry.register(MetricId.build("jvm-fd-ratio"), new FileDescriptorGaugeSet());

        final FastForwardReporter reporter = FastForwardReporter
            .forRegistry(registry)
            .prefix(APP_PREFIX)
            .schedule(TimeUnit.SECONDS, 5)
            .build();

        reporter.start();

        System.out.println("Sending jvm metrics...");
        System.in.read();

        reporter.stop();
    }
}
