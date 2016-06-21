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

package com.spotify.metrics.remote;

import com.google.common.collect.ImmutableMap;
import com.spotify.metrics.core.MetricId;

import java.util.Map;

/**
 * Utilities around building json blobs for sending to semantic aggregator.
 */
public class SemanticAggregator {

    public static Map<String, String> buildAttributes(MetricId id, String type) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.putAll(id.getTags());
        builder.put("metric_type", type);
        return builder.build();
    }

    public static Map<String, Object> buildDocument(
            String value, String key, Map<String, String> allAttributes) {
        return ImmutableMap.of(
                "type", "metric",
                "value", value,
                "key", key,
                "attributes", allAttributes);
    }
}
