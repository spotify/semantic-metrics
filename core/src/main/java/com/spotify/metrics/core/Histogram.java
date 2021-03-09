/*
 * Licensed to Ted Dunning under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Adapted from https://github.com/tdunning/t-digest/blob/c55e6ba9c717e07aa1e4dcb9489967b53963dd91/core/src/main/java/com/tdunning/math/stats/Histogram.java
 */

package com.spotify.metrics.core;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

/**
 * A Histogram is a histogram with cleverly chosen, but fixed, bin widths.
 * <p>
 * Different implementations may provide better or worse speed or space complexity,
 * but each is attuned to a particular distribution or error metric.
 */
@SuppressWarnings("WeakerAccess")
abstract class Histogram implements Serializable {
    protected LongAdder[] counts;
    protected double min;
    protected double max;

    Histogram(double min, double max) {
        this.min = min;
        this.max = max;
    }

    protected void setupBins(double min, double max) {
        int binCount = bucketIndex(max) + 1;
        if (binCount > 10000) {
            throw new IllegalArgumentException(
                    String.format("Excessive number of bins %d resulting from min,max = %.2g, %.2g",
                            binCount, min, max));

        }
        counts = new LongAdder[binCount];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = new LongAdder();
        }
    }

    void add(double v) {
        counts[bucket(v)].increment();
    }

    @SuppressWarnings("WeakerAccess")
    double[] getBounds() {
        double[] r = new double[counts.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = lowerBound(i);
        }
        return r;
    }

    long[] getCounts() {
        long[] counts = new long[this.counts.length];
        LongAdder[] longAdders = this.counts;
        for (int i = 0; i < longAdders.length; i++) {
            counts[i] = longAdders[i].longValue();
        }
        return counts;
    }

    long getTotalCount() {
        long total = 0;
        for (LongAdder count : this.counts) {
            total += count.longValue();
        }
        return total;
    }

    // exposed for testing
    int bucket(double x) {
        if (x <= min) {
            return 0;
        } else if (x >= max) {
            return counts.length - 1;
        } else {
            return bucketIndex(x);
        }
    }

    protected abstract int bucketIndex(double x);

    // exposed for testing
    abstract double lowerBound(int k);

    @SuppressWarnings("WeakerAccess")
    abstract long[] getCompressedCounts();

    @SuppressWarnings("WeakerAccess")
    abstract void writeObject(java.io.ObjectOutputStream out) throws IOException;

    @SuppressWarnings("WeakerAccess")
    abstract void readObject(java.io.ObjectInputStream in) throws IOException;

    abstract void add(Iterable<Histogram> others);
}