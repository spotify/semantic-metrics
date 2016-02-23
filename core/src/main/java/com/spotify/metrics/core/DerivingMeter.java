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

package com.spotify.metrics.core;

import com.codahale.metrics.Metered;

/**
 * A meter that takes the derivative of a value that is expected to be almost monotonically
 * increasing. A typical use case is to get the rate of change of a counter of the total number of
 * occurrences of something.
 * <p>
 * Implementations will ignore updates that are a decrease of the counter value. The rationale is
 * that the counter is expected to be monotonically increasing between infrequent resets (such as
 * when a process has been restarted). Thus, negative values should only happen on restart, and it
 * should be safe to discard those.
 */
public interface DerivingMeter extends Metered {
    /**
     * Indicates the current value of the counter and updates the underlying metrics. Decreasing
     * counter values are discarded, as is the first value reported.
     *
     * @param currentValue current counter value
     * @throws java.lang.IllegalArgumentException if the currentValue is negative
     */
    void mark(long currentValue);
}
