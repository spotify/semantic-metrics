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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of gauges for the number of threads in their various states and deadlock detection.
 */
public class ThreadStatesMetricSet implements SemanticMetricSet {
    // do not compute stack traces.
    private static final int STACK_TRACE_DEPTH = 0;

    private final ThreadMXBean threads;
    private final ThreadDeadlockDetector deadlockDetector;

    /**
     * Creates a new set of gauges using the default MXBeans.
     */
    public ThreadStatesMetricSet() {
        this(ManagementFactory.getThreadMXBean(), new ThreadDeadlockDetector());
    }

    /**
     * Creates a new set of gauges using the given MXBean and detector.
     *
     * @param threads a thread MXBean
     * @param deadlockDetector a deadlock detector
     */
    public ThreadStatesMetricSet(
        ThreadMXBean threads, ThreadDeadlockDetector deadlockDetector
    ) {
        this.threads = threads;
        this.deadlockDetector = deadlockDetector;
    }

    @Override
    public Map<MetricId, Metric> getMetrics() {
        final Map<MetricId, Metric> gauges = new HashMap<MetricId, Metric>();

        final MetricId threadState =
            MetricId.build().tagged("what", "jvm-thread-state", "unit", "thread");

        for (final Thread.State state : Thread.State.values()) {
            gauges.put(threadState.tagged("thread_state", state.toString()), new Gauge<Object>() {
                @Override
                public Object getValue() {
                    return getThreadCount(state);
                }
            });
        }

        gauges.put(threadState.tagged("thread_state", "deadlocked"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return deadlockDetector.getDeadlockedThreads().size();
            }
        });

        final MetricId threadType =
            MetricId.build().tagged("what", "jvm-thread-type", "unit", "thread");

        gauges.put(threadType.tagged("thread_type", "non-deamon"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return threads.getThreadCount() - threads.getDaemonThreadCount();
            }
        });

        gauges.put(threadType.tagged("thread_type", "daemon"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return threads.getDaemonThreadCount();
            }
        });

        return Collections.unmodifiableMap(gauges);
    }

    private int getThreadCount(Thread.State state) {
        final ThreadInfo[] allThreads = getThreadInfo();
        int count = 0;

        for (final ThreadInfo info : allThreads) {
            if (info != null && info.getThreadState() == state) {
                count++;
            }
        }

        return count;
    }

    private ThreadInfo[] getThreadInfo() {
        return threads.getThreadInfo(threads.getAllThreadIds(), STACK_TRACE_DEPTH);
    }
}
