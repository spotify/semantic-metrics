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

import com.spotify.metrics.core.ConcurrentDistribution;
import com.spotify.metrics.core.Distribution;
import com.spotify.metrics.core.SemanticMetricDistribution;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, warmups = 1)
@Measurement(time = 10, iterations = 5)
@Warmup(time = 10, iterations = 1)
public class DistributionBenchmark {

    private Distribution sync;
    private Distribution conc;

    @Setup
    public void setUp() {
        sync = new SemanticMetricDistribution();
        conc = new ConcurrentDistribution();
    }

    @Benchmark
    @GroupThreads(1)
    public void sync1() {
        sync.record(42.0);
    }

    @Benchmark
    @GroupThreads(2)
    public void sync2() {
        sync.record(42.0);
    }

    @Benchmark
    @GroupThreads(4)
    public void sync4() {
        sync.record(42.0);
    }

    @Benchmark
    @GroupThreads(8)
    public void sync8() {
        sync.record(42.0);
    }

    @Benchmark
    @GroupThreads(1)
    public void conc1() {
        conc.record(42.0);
    }

    @Benchmark
    @GroupThreads(2)
    public void conc2() {
        conc.record(42.0);
    }

    @Benchmark
    @GroupThreads(4)
    public void conc4() {
        conc.record(42.0);
    }

    @Benchmark
    @GroupThreads(8)
    public void conc8() {
        conc.record(42.0);
    }

}
