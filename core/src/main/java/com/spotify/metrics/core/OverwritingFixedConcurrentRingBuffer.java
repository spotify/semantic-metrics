/*
 * Copyright (c) 2020 Spotify AB.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This is a ringbuffer with a fixed capacity.
 * It needs to be pre-populated with a sentinel element.
 *
 * It can be used to maintain an up to date collection of recent writes.
 *
 * This class is thread-safe and lock-free.
 *
 * @param <T>
 */
public class OverwritingFixedConcurrentRingBuffer<T> {
    private final AtomicReferenceArray<T> buffer;
    private final int capacity;
    private final AtomicInteger position = new AtomicInteger(0);

    public OverwritingFixedConcurrentRingBuffer(final T sentinel, final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "capacity must be a positive integer but was: " + capacity);
        }
        this.buffer = new AtomicReferenceArray<>(capacity);
        this.capacity = capacity;

        for (int i = 0; i < capacity; i++) {
            buffer.set(i, sentinel);
        }
    }

    /**
     * Check if any object matches the predicate.
     * This will stop search after the first match.
     *
     * @param predicate
     * @return true if any element matches the predicate, otherwise false
     */
    public boolean anyMatch(final Predicate<T> predicate) {
        // Old elements are at the tail, which is just after the head,
        // so start searching there (though the tail keeps moving...)
        final int start = (position.get() & 0x7FFFFFFF) % capacity;
        for (int i = start; i < capacity; i++) {
            if (predicate.test(buffer.get(i))) {
                return true;
            }
        }

        // Scan the skipped part of the buffer too
        for (int i = 0; i < start; i++) {
            if (predicate.test(buffer.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Grabs a snapshot of the buffer. This is not an atomic operation,
     * and there are no ordering guarantees for the returned elements.
     *
     * @param filter a filter to apply for selecting elements.
     * @param mapper a mapper to apply to each filtered element to transform the type
     * @param <R> the return type
     * @return the snapshot
     */
    public <R> List<R> getSnapshot(final Predicate<T> filter, final Function<T, R> mapper) {
        final ArrayList<R> res = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            final T element = buffer.get(i);
            if (filter.test(element)) {
                res.add(mapper.apply(element));
            }
        }
        return res;
    }

    /**
     * Adds an element to the buffer. Exactly one other element (the oldest) will be evicted.
     *
     * @param element
     */
    public void add(final T element) {
        // Make sure this is not negative
        final int index = (position.getAndIncrement() & 0x7FFFFFFF) % capacity;
        buffer.set(index, element);
    }
}
