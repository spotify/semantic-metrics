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
/*
 * This class was initially copied and modified from the codahale metrics project.
 *
 * For the appropriate license (there was no header) see LICENSE.codahale.txt
 *
 * It was copied from the following tree:
 * https://github.com/dropwizard/metrics/tree/be6989bd082a033c2dd6a57b209f4a67584e3e1a
 */

package com.spotify.metrics.core;

import com.codahale.metrics.Metric;

/**
 * A filter used to determine whether or not a metric should be reported, among other things.
 */
public interface SemanticMetricFilter {
    /**
     * Matches all metrics, regardless of type or name.
     */
    SemanticMetricFilter ALL = new SemanticMetricFilter() {
        @Override
        public boolean matches(MetricId name, Metric metric) {
            return true;
        }
    };

    /**
     * Returns {@code true} if the metric matches the filter; {@code false} otherwise.
     *
     * @param name      the metric's name
     * @param metric    the metric
     * @return {@code true} if the metric matches the filter
     */
    boolean matches(MetricId name, Metric metric);
}
