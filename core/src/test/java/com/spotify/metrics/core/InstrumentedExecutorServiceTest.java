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

/*
 * This class was initially copied and modified from the codahale metrics project.
 *
 * For the appropriate license (there was no header) see LICENSE.codahale.txt
 *
 * It was copied from the following tree:
 * https://github.com/dropwizard/metrics/tree/6d1fff844b7fc8855b81bb42b7125bd84f3a3e7a
 */

package com.spotify.metrics.core;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentedExecutorServiceTest {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(InstrumentedExecutorServiceTest.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final SemanticMetricRegistry registry = new SemanticMetricRegistry();

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            LOGGER.error("InstrumentedExecutorService did not terminate.");
        }
    }

    @Test
    public void testReportsTasksInformationWithDefaultBaseMetricId()
        throws ExecutionException, InterruptedException {

        MetricId baseMetricId = new MetricId().tagged("executor", "executor-1");
        InstrumentedExecutorService instrumentedExecutorService =
            new InstrumentedExecutorService(executor, registry);

        testMetrics(instrumentedExecutorService, baseMetricId);
    }

    @Test
    public void testReportsTasksInformationWithBaseMetricId()
        throws ExecutionException, InterruptedException {

        MetricId baseMetricId = new MetricId().tagged("executor", "myexecutor");
        InstrumentedExecutorService instrumentedExecutorService =
            new InstrumentedExecutorService(executor, registry, baseMetricId);

        testMetrics(instrumentedExecutorService, baseMetricId);
    }

    private void testMetrics(
        ExecutorService executorService,
        MetricId baseMetricId) throws ExecutionException, InterruptedException {

        final Meter submitted = registry.meter(baseMetricId.tagged("what", "submitted"));
        final Counter running = registry.counter(baseMetricId.tagged("what", "running"));
        final Meter completed = registry.meter(baseMetricId.tagged("what", "completed"));
        final Timer duration = registry.timer(baseMetricId.tagged("what", "duration"));
        final Timer idle = registry.timer(baseMetricId.tagged("what", "idle"));

        assertEquals(submitted.getCount(), 0);
        assertEquals(running.getCount(), 0);
        assertEquals(completed.getCount(), 0);
        assertEquals(duration.getCount(), 0);
        assertEquals(idle.getCount(), 0);

        Future<?> future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                assertEquals(submitted.getCount(), 1);
                assertEquals(running.getCount(), 1);
                assertEquals(completed.getCount(), 0);
                assertEquals(duration.getCount(), 0);
                assertEquals(idle.getCount(), 1);
            }
        });

        future.get();

        assertEquals(submitted.getCount(), 1);
        assertEquals(running.getCount(), 0);
        assertEquals(completed.getCount(), 1);
        assertEquals(duration.getCount(), 1);
        assertEquals(duration.getSnapshot().size(), 1);
        assertEquals(idle.getCount(), 1);
        assertEquals(idle.getSnapshot().size(), 1);
    }
}
