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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A metric name with the ability to include semantic tags.
 * <p>
 * Instances of this class are immutable. There are methods that copy-then-modify MetricId's, these
 * should be used sparingly and not in performance critical sections since they are slow.
 * <p>
 * If you find the need to mutate a MetricId frequently, consider using {@link MetricIdMutator<T>}
 * which will cache values for you.
 * <p>
 * This replaces the previous style where metric names where strictly dot-separated strings.
 *
 * @author udoprog
 */
public class MetricId implements Comparable<MetricId> {
    public static final String SEPARATOR = ".";
    public static final SortedMap<String, String> EMPTY_TAGS =
        Collections.unmodifiableSortedMap(new TreeMap<String, String>());
    public static final MetricId EMPTY = new MetricId();

    private final String key;
    private final SortedMap<String, String> tags;
    // store hash since these objects are immutable and
    // it will speed up hashing/comparison operations.
    private final int hash;

    public MetricId() {
        this(null, EMPTY_TAGS);
    }

    public MetricId(String key) {
        this(key, EMPTY_TAGS);
    }

    public MetricId(String key, Map<String, String> tags) {
        this(key, Collections.unmodifiableSortedMap(new TreeMap<>(tags)));
    }

    MetricId(String key, SortedMap<String, String> tags) {
        this.key = key;
        this.tags = tags;
        this.hash = calculateHashCode(key, tags);
    }

    /**
     * Get the current key of the metric id.
     *
     * @return The key of the metric id.
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the current set of tags.
     *
     * @return The tags of the metric id.
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * Build the MetricName that is this with another path appended to it.
     * <p>
     * The new MetricName inherits the tags of this one.
     *
     * @param part The extra path element to add to the new metric.
     * @return A new metric name relative to the original by the path specified in p.
     */
    public MetricId resolve(final String part) {
        return new MetricId(extendKey(part), tags);
    }

    private String extendKey(final String part) {
        if (part == null || part.isEmpty()) {
            return key;
        }

        if (key == null || key.isEmpty()) {
            return part;
        }

        return key + SEPARATOR + part;
    }

    /**
     * Add tags to a metric name and return the newly created MetricName.
     *
     * @param add Tags to add.
     * @return A newly created metric name with the specified tags associated with it.
     */
    public MetricId tagged(Map<String, String> add) {
        final TreeMap<String, String> tags = new TreeMap<>(this.tags);
        tags.putAll(add);
        return new MetricId(key, tags);
    }

    /**
     * Same as {@link #tagged(Map)}, but takes a variadic list of arguments.
     *
     * @param pairs An even list of strings acting as key-value pairs.
     * @return A newly created metric name with the specified tags associated with it.
     * @see #tagged(Map)
     */
    public MetricId tagged(String... pairs) {
        if (pairs == null) {
            return this;
        }

        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Argument count must be even");
        }

        final Map<String, String> add = new TreeMap<>();

        for (int i = 0; i < pairs.length; i += 2) {
            add.put(pairs[i], pairs[i + 1]);
        }

        return tagged(add);
    }

    /**
     * Join the specified set of metric names.
     *
     * @param parts Multiple metric names to join using the separator.
     * @return A newly created metric name which has the name of the specified parts and includes
     * all tags of all child metric names.
     **/
    public static MetricId join(MetricId... parts) {
        final StringBuilder nameBuilder = new StringBuilder();
        final Map<String, String> tags = new HashMap<String, String>();

        boolean first = true;

        for (MetricId part : parts) {
            final String name = part.getKey();

            if (name != null && !name.isEmpty()) {
                if (first) {
                    first = false;
                } else {
                    nameBuilder.append(SEPARATOR);
                }

                nameBuilder.append(name);
            }

            if (!part.getTags().isEmpty()) {
                tags.putAll(part.getTags());
            }
        }

        return new MetricId(nameBuilder.toString(), tags);
    }

    /**
     * Build a new metric name using the specific path components.
     *
     * @param parts Path of the new metric name.
     * @return A newly created metric name with the specified path.
     **/
    public static MetricId build(final String... parts) {
        if (parts == null || parts.length == 0) {
            return MetricId.EMPTY;
        }

        if (parts.length == 1) {
            return new MetricId(parts[0], EMPTY_TAGS);
        }

        return new MetricId(key(parts), EMPTY_TAGS);
    }

    @Override
    public String toString() {
        return String.format("%s %s", key, tags);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != MetricId.class) {
            return false;
        }

        final MetricId m = ((MetricId) obj);

        // different hashes, fastest discriminator.
        if (hash != m.hash) {
            return false;
        }

        if (key == null || m.key == null) {
            if (key != m.key) {
                return false;
            }
        } else {
            if (!key.equals(m.key)) {
                return false;
            }
        }

        if (!tags.equals(m.tags)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(MetricId o) {
        if (o == null) {
            return -1;
        }

        // fast path, same object, or same content.
        if (this == o) {
            return 0;
        }

        // fast path, different hashes.
        final int h = Integer.compare(hash, o.hash);

        if (h != 0) {
            return h;
        }

        final int k = compareKey(key, o.getKey());

        if (k != 0) {
            return k;
        }

        return compareTags(tags.entrySet(), o.tags.entrySet());
    }

    private static String key(String... names) {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (final String name : names) {
            if (name == null || name.isEmpty()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                builder.append(SEPARATOR);
            }

            builder.append(name);
        }

        return builder.toString();
    }

    private int compareKey(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }

        if (left == null) {
            return 1;
        }

        if (right == null) {
            return -1;
        }

        return left.compareTo(right);
    }

    private int compareTags(
        Set<Map.Entry<String, String>> left, Set<Map.Entry<String, String>> right
    ) {
        if (left == null && right == null) {
            return 0;
        }

        if (left == null) {
            return 1;
        }

        if (right == null) {
            return -1;
        }

        final Iterator<Map.Entry<String, String>> li = left.iterator();
        final Iterator<Map.Entry<String, String>> ri = right.iterator();

        while (li.hasNext()) {
            if (!ri.hasNext()) {
                return -1;
            }

            final Map.Entry<String, String> l = li.next();
            final Map.Entry<String, String> r = ri.next();

            final int k = l.getKey().compareTo(r.getKey());

            if (k != 0) {
                return k;
            }

            final int v = l.getValue().compareTo(r.getValue());

            if (v != 0) {
                return v;
            }
        }

        if (ri.hasNext()) {
            return 1;
        }

        return 0;
    }

    private static int calculateHashCode(final String key, final SortedMap<String, String> tags) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + tags.hashCode();
        return result;
    }
}
