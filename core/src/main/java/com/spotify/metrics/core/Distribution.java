/*
 * Copyright (C) 2016 - 2020 Spotify AB.
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

package com.spotify.metrics.core;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Metric;

import com.google.protobuf.ByteString;
import com.tdunning.math.stats.TDigest;

import java.nio.ByteBuffer;


/**
 * {@link Distribution} is a simple interface that allows users to record measurements
 * to compute rank statistics on data distribution not just local source.
 *
 * <p>Every implementation should produce a serialized data sketch in a byteBuffer
 * as this metric point value. For more information on how this is handled upstream,
 * Please refer to
 * <a href="https://github.com/spotify/ffwd-client-java/blob/master/ffwd-
 *  client/src/main/java/com/spotify/ffwd/FastForward.java#L110"/> FastForward Java client</a>
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
     *
     * @return
     */
    default ByteString getValueAndFlush() {
        return getValue(getDigestAndFlush());
    }

    static ByteString getValue(final TDigest digest) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(digest.smallByteSize());
        digest.asSmallBytes(byteBuffer);
        return ByteString.copyFrom(byteBuffer.array());
    }

    TDigest getDigestAndFlush();

}
