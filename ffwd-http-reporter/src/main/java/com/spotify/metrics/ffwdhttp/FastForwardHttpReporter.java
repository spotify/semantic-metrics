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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import com.spotify.ffwd.http.HttpClient;
import com.spotify.ffwd.http.model.v2.Batch;
import com.spotify.ffwd.http.model.v2.Value;
import com.spotify.metrics.core.DerivingMeter;
import com.spotify.metrics.core.Distribution;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricFilter;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.tags.NoopTagExtractor;
import com.spotify.metrics.tags.TagExtractor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastForwardHttpReporter implements AutoCloseable {
    private static final String METRIC_TYPE = "metric_type";
    private static final Logger log = LoggerFactory.getLogger(FastForwardHttpReporter.class);

    private static final SemanticMetricFilter FILTER_ALL = SemanticMetricFilter.ALL;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            final Thread thread = new Thread(runnable);

            thread.setName(String.format("fast-forward-reporter-%d", count.getAndIncrement()));
            thread.setDaemon(true);

            return thread;
        }
    };

    private final ScheduledExecutorService executorService;
    private final boolean executorOwner;

    private final SemanticMetricRegistry registry;
    private final MetricId prefix;
    private final TimeUnit unit;
    private final long duration;
    private final HttpClient client;
    private final Set<Percentile> histogramPercentiles;
    private final Clock clock;
    private final TagExtractor tagExtractor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;

    private FastForwardHttpReporter(
            SemanticMetricRegistry registry, MetricId prefix, TimeUnit unit, long duration,
            HttpClient client, Set<Percentile> histogramPercentiles, Clock clock,
            TagExtractor tagExtractor,
            ScheduledExecutorService executorService, boolean executorOwner
    ) {
        this.registry = registry;
        this.prefix = prefix;
        this.unit = unit;
        this.duration = duration;
        this.client = client;
        this.histogramPercentiles = new HashSet<>(histogramPercentiles);
        this.clock = clock;
        this.tagExtractor = tagExtractor;
        this.executorService = executorService;
        this.executorOwner = executorOwner;
    }

    public static Builder forRegistry(SemanticMetricRegistry registry, HttpClient client) {
        return new Builder(registry, client);
    }

    public static final class Builder {
        private final SemanticMetricRegistry registry;
        private final HttpClient client;
        private long time = 5;
        private TimeUnit unit = TimeUnit.MINUTES;
        private MetricId prefix = MetricId.build();
        private Clock clock;
        private TagExtractor tagExtractor;

        private Set<Percentile> histogramPercentiles =
            Sets.newHashSet(new Percentile(0.75), new Percentile(0.99));
        private ScheduledExecutorService executorService;

        public Builder(SemanticMetricRegistry registry, HttpClient client) {
            this.registry = registry;
            this.client = client;
        }

        /**
         * The schedule at which metrics are reported.
         *
         * @param time the time of each reporting period
         * @param unit unit of the time
         * @return this builder
         */
        public Builder schedule(long time, TimeUnit unit) {
            this.time = time;
            this.unit = unit;
            return this;
        }

        /**
         * Key prefix to apply to all reported metrics.
         *
         * @return this builder
         */
        public Builder prefix(String prefix) {
            this.prefix = MetricId.build(prefix);
            return this;
        }

        /**
         * Prefix to apply to all reported metrics.
         *
         * @return this builder
         */
        public Builder prefix(MetricId prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Set which quantiles should be reported as percentiles by this reporter. Calling this
         * method overrides the default percentiles (p75, p99).
         *
         * @param quantiles values in range [0..1] that represent percentiles to be reported from
         * histograms, e.g., 0.75 means p75 should be reported
         * @return this builder
         */
        public Builder histogramQuantiles(double... quantiles) {
            histogramPercentiles = new HashSet<>();
            for (double q : quantiles) {
                histogramPercentiles.add(new Percentile(q));
            }
            return this;
        }

        /**
         * Clock implementation that will be used in the reporter.
         *
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * TagExtractor implementation that will be used in the reporter.
         *
         * @return this builder
         */
        public Builder tagExtractor(TagExtractor tagExtractor) {
            this.tagExtractor = tagExtractor;
            return this;
        }

        public Builder executorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public FastForwardHttpReporter build() throws IOException {
            Clock clock = this.clock;

            if (clock == null) {
                clock = new Clock.SystemTime();
            }

            final TagExtractor tagExtractor =
                this.tagExtractor != null ? this.tagExtractor : new NoopTagExtractor();

            final boolean executorOwner;
            final ScheduledExecutorService executorService;
            if (this.executorService != null) {
                executorService = this.executorService;
                executorOwner = false;
            } else {
                executorService = createExecutor();
                executorOwner = true;
            }
            return new FastForwardHttpReporter(registry, prefix, unit, time, client,
                histogramPercentiles, clock, tagExtractor, executorService, executorOwner);
        }

        private ScheduledExecutorService createExecutor() {
            return Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
        }

    }

    private void report() {
        final List<Batch.Point> points = new ArrayList<>();
        final long timestamp = clock.currentTimeMillis();

        for (@SuppressWarnings("rawtypes") Map.Entry<MetricId, Gauge> entry : registry
            .getGauges(FILTER_ALL)
            .entrySet()) {
            final BatchBuilder builder = createBuilder(points, timestamp, entry.getKey(), "gauge");
            reportGauge(builder, entry.getValue());
        }

        for (Map.Entry<MetricId, Counter> entry : registry.getCounters(FILTER_ALL).entrySet()) {
            final BatchBuilder builder =
                createBuilder(points, timestamp, entry.getKey(), "counter");
            reportCounter(builder, entry.getValue());
        }

        for (Map.Entry<MetricId, Histogram> entry : registry.getHistograms(FILTER_ALL).entrySet()) {
            final BatchBuilder builder =
                createBuilder(points, timestamp, entry.getKey(), "histogram");
            reportHistogram(builder, entry.getValue().getSnapshot());
        }

        for (Map.Entry<MetricId, Meter> entry : registry.getMeters(FILTER_ALL).entrySet()) {
            final BatchBuilder builder = createBuilder(points, timestamp, entry.getKey(), "meter");
            reportMeter(builder, entry.getValue());
        }

        for (Map.Entry<MetricId, Timer> entry : registry.getTimers(FILTER_ALL).entrySet()) {
            final BatchBuilder builder = createBuilder(points, timestamp, entry.getKey(), "timer");
            reportTimer(builder, entry.getValue());
        }

        for (Map.Entry<MetricId, DerivingMeter> entry : registry
            .getDerivingMeters(FILTER_ALL)
            .entrySet()) {
            final BatchBuilder builder =
                createBuilder(points, timestamp, entry.getKey(), "deriving-meter");
            reportDerivingMeter(builder, entry.getValue());
        }

        for (Map.Entry<MetricId, Distribution> entry :
                registry.getDistributions(FILTER_ALL).entrySet()) {
            final BatchBuilder builder = createBuilder(points, timestamp, entry.getKey(),
                    "distribution");
            reportDistribution(builder, entry.getValue());
        }

        final Map<String, String> commonTags = tagExtractor.addTags(prefix.getTags());
        final Batch batch = new Batch(commonTags, createResource(), points);

        client.sendBatch(batch).toCompletable().await();
    }

    private BatchBuilder createBuilder(
        final List<Batch.Point> points, final long timestamp, final MetricId id,
        final String metricType
    ) {
        final String key = joinKeys(prefix, id);
        final String unit = getUnit(id.getTags());
        return new BatchBuilder(points, timestamp, key, id.getTags(), unit, metricType);
    }

    private static Map<String, String> createResource() {
        return new HashMap<>();
    }

    private void reportGauge(
            final BatchBuilder builder, @SuppressWarnings("rawtypes") Gauge value
    ) {
        if (value == null) {
            return;
        }

        builder.buildPoint(null, convert(value.getValue()));
    }

    private void reportDistribution(BatchBuilder builder, Distribution distribution) {
        if (distribution.getCount() == 0) {
            return;
        }
        builder.buildPoint("distribution", distribution.getValueAndFlush());
    }

    private double convert(Object value) {
        if (value instanceof Number) {
            return Number.class.cast(value).doubleValue();
        }

        return 0;
    }

    private void reportCounter(
        final BatchBuilder builder, final Counting value
    ) {
        builder.buildPoint("count", value.getCount());
    }

    private void reportMeter(
        final BatchBuilder builder, Meter value
    ) {
        reportMetered(builder, value);
        reportCounter(builder, value);
    }

    private void reportTimer(
        final BatchBuilder builderIn, Timer value
    ) {
        final BatchBuilder builder = builderIn.withUnit("ns");
        reportMetered(builder, value);
        reportHistogram(builder, value.getSnapshot());
    }

    private void reportDerivingMeter(
        final BatchBuilder builder, DerivingMeter value
    ) {
        reportMetered(builder, value);
    }

    private void reportHistogram(
        final BatchBuilder builder, final Snapshot s
    ) {
        builder.buildPoint("min", s.getMin());
        builder.buildPoint("max", s.getMax());
        builder.buildPoint("mean", s.getMean());
        builder.buildPoint("median", s.getMedian());
        builder.buildPoint("stddev", s.getStdDev());

        reportHistogramQuantiles(builder, s);
    }

    private void reportHistogramQuantiles(
        final BatchBuilder builder, final Snapshot s
    ) {
        for (Percentile q : histogramPercentiles) {
            builder.buildPoint(q.getPercentileString(), s.getValue(q.getQuantile()));
        }
    }

    private void reportMetered(
        final BatchBuilder builder, final Metered value
    ) {
        final BatchBuilder b = builder.withUnit(builder.getUnit() + "/s");

        b.buildPoint("1m", value.getOneMinuteRate());
        b.buildPoint("5m", value.getFiveMinuteRate());
    }

    private String getUnit(final Map<String, String> tags) {
        final String unit = tags.get("unit");

        if (unit == null) {
            return "n";
        }

        return unit;
    }

    private String joinKeys(MetricId... parts) {
        final StringBuilder key = new StringBuilder();

        for (final MetricId part : parts) {
            final String name = part.getKey();

            if (name != null && !name.isEmpty()) {
                if (key.length() > 0) {
                    key.append('.');
                }

                key.append(name);
            }
        }

        return key.toString();
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }

        scheduledFuture = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    FastForwardHttpReporter.this.report();
                } catch (final Exception e) {
                    log.error("Error when trying to report metrics", e);
                }
            }
        }, 0, duration, unit);
    }

    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        if (executorOwner) {
            executorService.shutdown();
        }
    }

    @Override
    public void close() {
        stop();
    }

    private static class BatchBuilder {
        private final List<Batch.Point> points;
        private final long timestamp;
        private final String key;
        private final Map<String, String> tags;
        private final String unit;
        private final String metricType;

        public BatchBuilder(
            final List<Batch.Point> points, final long timestamp, final String key,
            final Map<String, String> tags, final String unit, final String metricType
        ) {
            this.points = points;
            this.timestamp = timestamp;
            this.key = key;
            this.tags = tags;
            this.unit = unit;
            this.metricType = metricType;
        }

        public String getUnit() {
            return unit;
        }

        public BatchBuilder withUnit(final String unit) {
            return new BatchBuilder(points, timestamp, key, tags, unit, metricType);
        }

        public BatchBuilder withMetricType(final String metricType) {
            return new BatchBuilder(points, timestamp, key, tags, unit, metricType);
        }

        public void buildPoint(final String stat, final double value) {
            points.add(new Batch.Point(key, statsMap(stat), createResource(),
                    Value.DoubleValue.create(value), timestamp));
        }

        public void buildPoint(final String stat, final ByteString value) {
            points.add(new Batch.Point(key, statsMap(stat), createResource(),
                    Value.DistributionValue.create(value), timestamp));
        }

        private Map<String, String> statsMap(
            final String stat
        ) {
            final boolean sameUnit = this.unit.equals(tags.get("unit"));
            final boolean sameMetricType = this.metricType.equals(tags.get("unit"));

            if (sameMetricType && sameUnit && stat == null) {
                return tags;
            }

            final Map<String, String> builder = new HashMap<>(tags);

            if (!sameUnit) {
                builder.put("unit", this.unit);
            }

            if (stat != null) {
                builder.put("stat", stat);
            }

            if (!sameMetricType) {
                builder.put("metric_type", metricType);
            }

            return builder;
        }
    }
}
