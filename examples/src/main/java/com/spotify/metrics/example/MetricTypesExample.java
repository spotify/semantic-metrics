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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwd.FastForwardReporter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This example demonstrates how to use different metric types.
 * <p>
 * To run this example and see the output please setup a local ffwd and run ffwd debugger. Follow
 * this link to see the instruction: https://github
 * .com/spotify/ffwd/blob/master/docs/basic-local-ffwd.md
 */
public class MetricTypesExample {

    private static final MetricId APP_PREFIX = MetricId.build("metric-types-example");
    private static final SemanticMetricRegistry registry = new SemanticMetricRegistry();

    /**
     * A gauge is an instantaneous measurement of a value. We want to measure the number of pending
     * jobs in a queue
     */
    private static void reportGauge() {
        // Create or fetch (if it is already created) the metric.
        registry.register(APP_PREFIX.tagged("what", "job-queue-length"), new Gauge<Integer>() {

            @Override
            public Integer getValue() {
                // fetch the queue length the way you like
                final int queueLength = 10;
                // obviously this is gonna keep reporting 10, but you know ;)

                return queueLength;
            }
        });

        // That's it! The rest will be automatically done inside semantic metrics library.
        // Every time the reporter wants to report, getValue method of the Gauge will be invoked,
        // and a datapoint will be created and reported
    }

    /**
     * A counter is just a gauge for an AtomicLong instance. You can increment or decrement its
     * value. We want a more efficient way of measuring the pending job in a queue
     */
    private static void reportCounter() {
        // Create or fetch (if it is already created) the metric.
        final Counter counter = registry.counter(APP_PREFIX.tagged("what", "job-count"));

        // Somewhere in your code where you are adding new jobs to the queue you increment the
        // counter as well
        counter.inc();

        // Oh look! Another job!
        counter.inc();

        // Somewhere in your code the job is going to be removed from the queue you decrement the
        // counter
        counter.dec();

        // That's it! The rest will be automatically done inside semantic metrics library. The
        // reported measurements will be kept in the registry.
        // Every time the reporter wants to report, the current value of the counter will be read
        // and
        // a datapoint will be created and reported.
    }

    /**
     * A meter measures the rate of events over time (e.g., “requests per second”). In addition to
     * the mean rate, meters also track 1-, 5-, and 15-minute moving averages.
     * <p>
     * We have an endpoint that we want to measure how frequent we receive requests for it. this
     * method demonstrates how to do that.
     */
    private static void reportMeter() {
        // Create or fetch (if it is already created) the metric.
        final Meter meter = registry.meter(
            APP_PREFIX.tagged("what", "incoming-requests").tagged("endpoint", "/v1/list"));

        // Now a request comes and it's time to mark the meter
        meter.mark();

        // That's it! The rest will be automatically done inside semantic metrics library. The
        // reported measurements will be kept in the registry.
        // Every time the reporter wants to report, different stats and aggregations (1-, 5-, and
        // 15-minute moving averages) will be calculated and
        // datapoints will be created and reported.
    }

    /**
     * A histogram measures the statistical distribution of values in a stream of data. In addition
     * to minimum, maximum, mean, etc., it also measures median, 75th, 90th, 95th, 98th, 99th, and
     * 99.9th percentiles. This histogram will measure the size of responses in bytes.
     */
    private static void reportHistogram() {
        // Create or fetch (if it is already created) the metric.
        final Histogram histogram = registry.histogram(
            APP_PREFIX.tagged("what", "response-size").tagged("endpoint", "/v1/content"));

        // fetch the size of the response
        final long responseSize = 1000;
        // obviously this is gonna keep reporting 1000, but you know ;)

        histogram.update(responseSize);

        // That's it! The rest will be automatically done inside semantic metrics library. The
        // reported measurements will be kept in the registry.
        // Every time the reporter wants to report, different stats and aggregations (min, max,
        // median, 75th, 90th, 95th, 98th, 99th, and 99.9th percentiles) will be calculated and
        // datapoints will be created and reported.
    }

    /**
     * A timer measures both the rate that a particular piece of code is called and the distribution
     * of its duration. For example we want to measure the rate and handling duration of incoming
     * requests.
     */
    private static void reportTimer() {
        // Create or fetch (if it is already created) the metric.
        final Timer timer = registry.timer(
            APP_PREFIX.tagged("what", "incoming-request-time").tagged("endpoint", "/v1/get_stuff"));

        // Do this before starting to do the thing. This creates a measurement context object
        // that you can pass around.
        final Context context = timer.time();

        // Do stuff that takes time (e.g., process the request)
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        // Tell the context that it's done. This will register the duration and counts one
        // occurrence.
        context.stop();

        // That's it! The rest will be automatically done inside semantic metrics library. The
        // reported measurements will be kept in the registry.
        // Every time the reporter wants to report, different stats and aggregations (all the
        // stats that you would get from a meter and a histogram are included) will be calculated
        // and
        // datapoints will be created and reported.
    }

    public static void main(final String[] args) throws IOException {
        final FastForwardReporter reporter =
            FastForwardReporter.forRegistry(registry).schedule(TimeUnit.SECONDS, 5).build();
        reporter.start();

        reportGauge();
        reportCounter();
        reportMeter();
        reportHistogram();
        reportTimer();

        System.out.println("Sending metrics...");
        System.in.read();

        reporter.stop();
    }
}
