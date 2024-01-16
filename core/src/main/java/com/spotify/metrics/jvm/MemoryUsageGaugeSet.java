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
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of gauges for JVM memory usage, including stats on heap vs. non-heap memory, plus
 * GC-specific memory pools.
 */
public class MemoryUsageGaugeSet implements SemanticMetricSet {
    private final MemoryMXBean m;
    private final List<MemoryPoolMXBean> pools;

    public MemoryUsageGaugeSet() {
        this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans());
    }

    public MemoryUsageGaugeSet(MemoryMXBean m, Collection<MemoryPoolMXBean> pools) {
        this.m = m;
        this.pools = new ArrayList<MemoryPoolMXBean>(pools);
    }

    @Override
    public Map<MetricId, Metric> getMetrics() {
        final Map<MetricId, Metric> gauges = new HashMap<MetricId, Metric>();
        final MetricId memory = MetricId.build().tagged("what", "jvm-memory-usage", "unit", "B");

        putGauges(gauges, memory.tagged("memory", "heap"), new MemoryUsageSupplier() {
            @Override
            public MemoryUsage get() {
                return m.getHeapMemoryUsage();
            }
        });
        putGauges(gauges, memory.tagged("memory", "non-heap"), new MemoryUsageSupplier() {
            @Override
            public MemoryUsage get() {
                return m.getNonHeapMemoryUsage();
            }
        });

        for (final MemoryPoolMXBean m : pools) {
            putGauges(gauges, memory.tagged("memory", m.getName()), new MemoryUsageSupplier() {
                @Override
                public MemoryUsage get() {
                    return m.getUsage();
                }
            });
        }

        return Collections.unmodifiableMap(gauges);
    }

    private void putGauges(
        final Map<MetricId, Metric> gauges, final MetricId nonHeap,
        final MemoryUsageSupplier memoryUsageSupplier
    ) {
        gauges.put(nonHeap.tagged("memory_category", "init"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryUsageSupplier.get().getInit();
            }
        });

        gauges.put(nonHeap.tagged("memory_category", "used"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryUsageSupplier.get().getUsed();
            }
        });

        gauges.put(nonHeap.tagged("memory_category", "max"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryUsageSupplier.get().getMax();
            }
        });

        gauges.put(nonHeap.tagged("memory_category", "committed"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryUsageSupplier.get().getCommitted();
            }
        });

        gauges.put(nonHeap.tagged("memory_category", "used_ratio"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                if (memoryUsageSupplier.get().getMax() <= 0) {
                    return 1L;
                }
                return memoryUsageSupplier.get().getUsed() / memoryUsageSupplier.get().getMax();
            }
        });
    }

    private interface MemoryUsageSupplier {
        MemoryUsage get();
    }
}
