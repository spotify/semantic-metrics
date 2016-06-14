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

import java.util.List;
import java.util.Map;

/**
 * Utilities around sharding monitoring events.
 */
public class Sharder {

    public static final String SHARD_KEY = "X-Shard-Key";

    public static String buildShardKey(List<String> shardKey, Map<String, String> values) {
        StringBuilder shardBuilder = new StringBuilder();
        for (String key : shardKey) {
            if (shardBuilder.length() != 0) {
                shardBuilder.append(',');
            }
            shardBuilder.append(httpHeaderEscape(key));
            shardBuilder.append(':');
            shardBuilder.append(httpHeaderEscape(values.get(key)));
        }
        return shardBuilder.toString();
    }

    public static String httpHeaderEscape(String key) {
        StringBuilder res = new StringBuilder();
        for (char c : key.toCharArray()) {
            if (((c >= 'a' && c <= 'z')) || ((c >= 'A' && c <= 'Z'))) {
                res.append(c);
            }
        }
        return res.toString();
    }

}
