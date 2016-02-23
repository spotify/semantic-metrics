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

import com.codahale.metrics.Histogram;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwd.FastForwardReporter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CustomPercentiles {
    private static final MetricId APP_PREFIX = MetricId.build("custom-percentile-example");
    private static final SemanticMetricRegistry registry = new SemanticMetricRegistry();

    public static void main(String[] args) throws IOException {
        FastForwardReporter f = FastForwardReporter
            .forRegistry(registry)
            .histogramQuantiles(0.62, 0.55, 0.99)
            .schedule(TimeUnit.SECONDS, 10)
            .build();
        f.start();

        Histogram h = registry.histogram(APP_PREFIX.tagged("what", "stuff"));

        for (int i = 0; i < 100; i++) {
            h.update(i);
        }

        System.out.println("Sending custom percentiles for histogram...");
        System.in.read();
        f.stop();
    }
}
