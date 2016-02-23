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

import com.codahale.metrics.Meter;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwd.FastForwardReporter;

import java.util.concurrent.TimeUnit;

/**
 * An example application that generates metrics with dynamic tags
 * <p>
 * To run this example and see the output please setup a local ffwd and run ffwd debugger. Follow
 * this link to see the instruction: https://github
 * .com/spotify/ffwd/blob/master/docs/basic-local-ffwd.md
 */
public class DynamicMetricExample {
    private static final MetricId APP_PREFIX = MetricId.build("dynamic-metric-example");
    private static final SemanticMetricRegistry registry = new SemanticMetricRegistry();

    public static void main(final String[] args) throws Exception {
        final MetricId base = MetricId.build("current-thing");

        final FastForwardReporter reporter = FastForwardReporter
            .forRegistry(registry)
            .prefix(APP_PREFIX)
            .schedule(TimeUnit.SECONDS, 5)
            .build();

        reporter.start();

        final String[] things = {
            "shoe", "bucket", "chair"
        };

        for (int i = 0; i < things.length; i++) {
            final String thing = things[i];
            Thread.sleep(1000);
            final Meter meter =
                registry.meter(base.tagged("thing", thing).tagged("index", Integer.toString(i)));
            meter.mark(10 * i);
        }

        System.out.println("Sending dynamic metrics...");
        System.in.read();

        reporter.stop();
    }
}
