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

package com.spotify.metrics.core.codahale.metrics.ext;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Metric;

import java.nio.ByteBuffer;

/**
 * {@link Distribution} is a simple interface that allows users to record measurements
 * to compute rank statistics on data distribution not just local source.
 *
 * <p>Unlike traditional histogram, {@link Distribution} doesn't require
 * predefined percentile value. Data recorded
 * can be used upstream to compute any percentile.
 *
 * <p>This Distribution doesn't require any binning configuration.
 * Just get an instance through SemanticMetricBuilder and record data.
 *
 * <p> {@link Distribution} is a good choice if you care about percentile accuracy in
 * a distributed environment and you want to rely on P99 to set SLO.
 */
public interface Distribution extends Metric, Counting {

    /**
     * Record value from Min.Double to Max.Double.
     * @param val
     */
    void record(double val);

    /**
     * Return distribution point value and flush.
     * When this method is called every internal state
     * is reset and a new recording starts.
     * @return
     */
    ByteBuffer getValueAndFlush();

}
