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

import com.spotify.ffwd.http.HttpClient;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwdhttp.FastForwardHttpReporter;


public class FfwdHttpReportExample {
    private static final MetricId APP_PREFIX = MetricId.build("distribution-metric-example");
    private static final SemanticMetricRegistry registry = new SemanticMetricRegistry();

    public static void main(final String[] args) throws Exception {
        final HttpClient.Builder builder = new HttpClient.Builder();
        final HttpClient httpClient = builder.build();

        //client should send a batch of 5 data points
        for (int i = 0; i < 1000; i++) {
            registry.distribution(MetricId.build("distributionExample")
                    .tagged("what", "service-latency").tagged("host",
                            "host" + i / 200)).record(Math.random());
        }

        final FastForwardHttpReporter reporter = FastForwardHttpReporter
                .forRegistry(registry, httpClient)
                .prefix(APP_PREFIX)
                .build();


        reporter.start();

        System.out.println("Sending dynamic metrics...");
        System.in.read();

    }
}

