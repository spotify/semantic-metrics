/*
 * Copyright (c) 2018 Spotify AB.
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

package com.spotify.metrics.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Extract resources from the system environment variables.
 *
 */
public class EnvironmentResourceTagExtractor implements ResourceExtractor {

    /**
     * Prefix of environment variable that adds additional resources.
     */
    private static final String FFWD_RESOURCE_PREFIX = "FFWD_RESOURCE_";

    private static Map<String, String> environmentResources;

    EnvironmentResourceTagExtractor(Supplier<Map<String, String>> environmentSupplier) {
        environmentResources = filterEnvironmentResources(environmentSupplier.get());
    }

    /**
     * Extract resources from the system environment variables.
     * Resources extracted from the environment takes precedence and overwrites existing resources
     * with the same key.
     *
     * @return map with extracted resources added.
     */
    @Override
    public Map<String, String> addResources(Map<String, String> resources) {
        final Map<String, String> extractedResources = new HashMap<>(resources);
        extractedResources.putAll(environmentResources);
        return extractedResources;
    }

    /**
     * Extract resources from a map that can correspond to system environment variables.
     *
     * @return extracted resources.
     */
    static Map<String, String> filterEnvironmentResources(final Map<String, String> env) {
        final Map<String, String> resources = new HashMap<>();

        for (final Map.Entry<String, String> e : env.entrySet()) {
            if (e.getKey().startsWith(FFWD_RESOURCE_PREFIX)) {
                final String tag = e.getKey().substring(FFWD_RESOURCE_PREFIX.length());
                resources.put(tag.toLowerCase(), e.getValue());
            }
        }

        return resources;
    }

}
