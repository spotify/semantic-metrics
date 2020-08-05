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

package com.spotify.metrics.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Extract tags from the system environment variables.
 *
 */
public class EnvironmentTagExtractor implements TagExtractor {

    /**
     * Prefix of environment variable that adds additional tags.
     */
    private static final String FFWD_TAG_PREFIX = "FFWD_TAG_";

    private static Map<String, String> environmentTags;

    public EnvironmentTagExtractor(Supplier<Map<String, String>> environmentSupplier) {
        environmentTags = filterEnvironmentTags(environmentSupplier.get());
    }

    /**
     * Extract tags from the system environment variables.
     * Tags extracted from the environment takes precedence and overwrites existing tags
     * with the same key.
     *
     * @return map with extracted tags added.
     */
    @Override
    public Map<String, String> addTags(Map<String, String> tags) {
        final Map<String, String> extractedTags = new HashMap<>(tags);
        extractedTags.putAll(environmentTags);
        return extractedTags;
    }

    /**
     * Extract tags from a map that can correspond to system environment variables.
     *
     * @return extracted tags.
     */
    static Map<String, String> filterEnvironmentTags(final Map<String, String> env) {
        final Map<String, String> tags = new HashMap<>();

        for (final Map.Entry<String, String> e : env.entrySet()) {
            if (e.getKey().startsWith(FFWD_TAG_PREFIX)) {
                final String tag = e.getKey().substring(FFWD_TAG_PREFIX.length());
                tags.put(tag.toLowerCase(), e.getValue());
            }
        }

        return tags;
    }
}
