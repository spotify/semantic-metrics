/*
 * Copyright (C) 2021 Spotify AB.
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

package com.spotify.metrics.jmh;

import com.codahale.metrics.Histogram;
import com.spotify.metrics.core.Distribution;
import com.spotify.metrics.core.SemanticMetricBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2, warmups = 1)
@Measurement(time = 10, iterations = 5)
@Warmup(time = 10, iterations = 2)
public class DistributionBenchmark {

    private Distribution distribution;
    private Histogram histogram;

    @Setup
    public void setUp() {
        distribution = SemanticMetricBuilder.DISTRIBUTION.newMetric();
        histogram = SemanticMetricBuilder.HISTOGRAMS.newMetric();
    }

    @Benchmark
    @Group("dist1")
    @GroupThreads(1)
    public void distThreads1() {
        distribution.record(42.0);
    }

    @Benchmark
    @Group("dist2")
    @GroupThreads(2)
    public void dist2() {
        distribution.record(42.0);
    }

    @Benchmark
    @Group("dist4")
    @GroupThreads(4)
    public void distThreads4() {
        distribution.record(42.0);
    }

    @Benchmark
    @Group("dist8")
    @GroupThreads(8)
    public void distThreads8() {
        distribution.record(42.0);
    }
    @Benchmark
    @Group("hist1")
    @GroupThreads(1)
    public void histThreads1() {
        histogram.update(42);
    }

    @Benchmark
    @Group("hist2")
    @GroupThreads(2)
    public void hist2() {
        histogram.update(42);
    }

    @Benchmark
    @Group("hist4")
    @GroupThreads(4)
    public void histThreads4() {
        histogram.update(42);
    }

    @Benchmark
    @Group("hist8")
    @GroupThreads(8)
    public void histThreads8() {
        histogram.update(42);
    }

}
