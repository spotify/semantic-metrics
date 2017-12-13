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

package com.spotify.metrics.ffwdhttp;

import com.google.common.base.Strings;

public class Percentile {
    private final int whole;
    private final int decimal;

    private final String percentileString;
    private final double quantile;

    /**
     * Construct a percentile based on the whole part and the decimal part of a number. e.g.,
     * whole=99, decimal=8 => p99.8
     *
     * @param whole a value in range [0..100) that represents a percentile, e.g., 75 means p75
     * @param decimal a value in range [0..9] that represents the decimal digit of the percentile
     */
    public Percentile(int whole, int decimal) {
        if (whole < 0 || whole >= 100) {
            throw new IllegalArgumentException("whole " + whole + " is not in [0..100)");
        }
        if (decimal < 0 || decimal > 9) {
            throw new IllegalArgumentException("decimal " + decimal + " is not in [0..9]");
        }
        this.whole = whole;
        this.decimal = decimal;
        percentileString = toPercentileString(this.whole, this.decimal);
        quantile = toQuantile(this.whole, this.decimal);
    }

    /**
     * @param whole a value in range [0..100) that represents a percentile, e.g., 75 means p75
     */
    public Percentile(int whole) {
        this(whole, 0);
    }

    /**
     * @param quantile a value in range [0..1) that represents a percentile, e.g., 0.75 means p75.
     * Note that it will be rounded to have 3 decimal digits, e.g., 0.7558 -> 0.756
     */
    public Percentile(double quantile) {
        //        Here is what is happening:

        //        p = quantile * 100;
        //        int w = (int) p;
        //        int d = (int)Math.round((p - w) * 10);
        //        this(w, d);

        this((int) (quantile * 100),
            (int) Math.round(((quantile * 100) - (int) (quantile * 100)) * 10));
    }

    private String toPercentileString(int whole, int decimal) {
        String pString = "p" + whole;
        if (decimal != 0) {
            return pString + "." + decimal;
        }
        return pString;
    }

    private double toQuantile(int whole, int decimal) {
        return Double.parseDouble("0." + Strings.padStart("" + whole, 2, '0') + decimal);
    }

    public double getQuantile() {
        return quantile;
    }

    public String getPercentileString() {
        return percentileString;
    }

    @Override
    public String toString() {
        return percentileString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + decimal;
        result = prime * result + whole;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Percentile other = (Percentile) obj;
        if (decimal != other.decimal) {
            return false;
        }
        if (whole != other.whole) {
            return false;
        }
        return true;
    }
}
