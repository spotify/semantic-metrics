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

package com.spotify.metrics.ffwd;

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
import com.spotify.ffwd.FastForward;
import com.spotify.ffwd.Metric;
import com.spotify.ffwd.v1.Value;
import com.spotify.metrics.core.DerivingMeter;
import com.spotify.metrics.core.Distribution;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricFilter;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.tags.NoopTagExtractor;
import com.spotify.metrics.tags.TagExtractor;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastForwardReporter implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FastForwardReporter.class);

    private static final String METRIC_TYPE = "metric_type";

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
    private final FastForward client;
    private final TagExtractor tagExtractor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Set<Percentile> histogramPercentiles;
    private ScheduledFuture<?> scheduledFuture;

    private FastForwardReporter(
        SemanticMetricRegistry registry, MetricId prefix, TimeUnit unit, long duration,
        FastForward client, Set<Percentile> histogramPercentiles, TagExtractor tagExtractor,
        ScheduledExecutorService executorService,
        boolean executorOwner) {
        this.registry = registry;
        this.prefix = prefix;
        this.unit = unit;
        this.duration = duration;
        this.client = client;
        this.histogramPercentiles = new HashSet<>(histogramPercentiles);
        this.tagExtractor = tagExtractor;
        this.executorService = executorService;
        this.executorOwner = executorOwner;
    }

    public static Builder forRegistry(SemanticMetricRegistry registry) {
        return new Builder(registry);
    }

    public static final class Builder {
        private final SemanticMetricRegistry registry;
        private TimeUnit unit = TimeUnit.MINUTES;
        private long time = 5;
        private String host = FastForward.DEFAULT_HOST;
        private int port = FastForward.DEFAULT_PORT;
        private MetricId prefix = MetricId.build();
        private FastForward client = null;
        private TagExtractor tagExtractor;
        private ScheduledExecutorService executorService;

        private Set<Percentile> histogramPercentiles =
            Sets.newHashSet(new Percentile(0.75), new Percentile(0.99));

        public Builder(SemanticMetricRegistry registry) {
            this.registry = registry;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder schedule(TimeUnit unit, long time) {
            this.unit = unit;
            this.time = time;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = MetricId.build(prefix);
            return this;
        }

        public Builder prefix(MetricId prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder fastForward(FastForward client) {
            this.client = client;
            return this;
        }

        public Builder tagExtractor(TagExtractor tagExtractor) {
            this.tagExtractor = tagExtractor;
            return this;
        }

        public Builder executorService(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        /**
         * Set which quantiles should be reported as percentiles by this reporter. Calling this
         * method overrides the default percentiles (p75, p99).
         *
         * @param quantiles values in range [0..1] that represent percentiles to be reported from
         * histograms, e.g., 0.75 means p75 should be reported
         */
        public Builder histogramQuantiles(double... quantiles) {
            histogramPercentiles = new HashSet<>();
            for (double q : quantiles) {
                histogramPercentiles.add(new Percentile(q));
            }
            return this;
        }

        public FastForwardReporter build() throws IOException {
            final FastForward client =
                this.client != null ? this.client : FastForward.setup(host, port);
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
            return new FastForwardReporter(registry, prefix, unit, time, client,
                histogramPercentiles, tagExtractor, executorService, executorOwner);
        }

        private ScheduledExecutorService createExecutor() {
            return Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
        }
    }

    public void report() {
        report(registry.getGauges(FILTER_ALL), registry.getCounters(FILTER_ALL),
            registry.getHistograms(FILTER_ALL), registry.getMeters(FILTER_ALL),
            registry.getTimers(FILTER_ALL), registry.getDerivingMeters(FILTER_ALL),
            registry.getDistributions(FILTER_ALL));
    }

    private void report(
        @SuppressWarnings("rawtypes") SortedMap<MetricId, Gauge> gauges,
        SortedMap<MetricId, Counter> counters, SortedMap<MetricId, Histogram> histograms,
        SortedMap<MetricId, Meter> meters, SortedMap<MetricId, Timer> timers,
        SortedMap<MetricId, DerivingMeter> derivingMeters,
        SortedMap<MetricId, Distribution> distributions
    ) {
        for (@SuppressWarnings("rawtypes") Map.Entry<MetricId, Gauge> entry : gauges.entrySet()) {
            reportGauge(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<MetricId, Counter> entry : counters.entrySet()) {
            reportCounter(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<MetricId, Histogram> entry : histograms.entrySet()) {
            reportHistogram(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<MetricId, Meter> entry : meters.entrySet()) {
            reportMetered(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<MetricId, Timer> entry : timers.entrySet()) {
            reportTimer(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<MetricId, DerivingMeter> entry : derivingMeters.entrySet()) {
            reportDerivingMeter(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<MetricId, Distribution> entry : distributions.entrySet()) {
            reportDistribution(entry.getKey(), entry.getValue());
        }
    }

    private void reportGauge(
        MetricId key, @SuppressWarnings("rawtypes") Gauge value
    ) {

        key = MetricId.join(prefix, key);

        final Metric m = FastForward
            .metric(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "gauge");

        send(m.value(convert(value.getValue())));
    }

    private double convert(Object value) {
        if (value instanceof Number) {
            return Number.class.cast(value).doubleValue();
        }

        return 0;
    }

    private void reportCounter(MetricId key, Counting value) {
        key = MetricId.join(prefix, key);

        final Metric m = FastForward
            .metric(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "counter");

        send(m.value(value.getCount()));
    }

    private void reportHistogram(MetricId key, Histogram value) {
        key = MetricId.join(prefix, key);

        final Metric m = FastForward
            .metric(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "histogram");

        reportHistogram(m, value.getSnapshot());
    }

    private void reportMetered(MetricId key, Meter value) {
        MetricId originalKey = key;
        key = MetricId.join(prefix, key);

        final Metric m = FastForward
            .metric(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "meter");

        reportMetered(m, value);
        reportCounter(originalKey, value);
    }

    private void reportTimer(MetricId key, Timer value) {
        key = MetricId.join(prefix, key);

        final Metric m = FastForward
            .metric(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "timer")
            .attribute("unit", "ns");

        reportMetered(m, value);
        reportHistogram(m, value.getSnapshot());
    }

    private void reportDerivingMeter(MetricId key, DerivingMeter value) {
        key = MetricId.join(prefix, key);

        final Metric m = FastForward
            .metric(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "deriving-meter");

        reportMetered(m, value);
    }

    private void reportDistribution(MetricId key, Distribution distribution) {
        key = MetricId.join(prefix, key);

        final com.spotify.ffwd.v1.Metric metric = FastForward
            .metricV1(key.getKey())
            .attributes(key.getTags())
            .attribute(METRIC_TYPE, "distribution");

        reportDistribution(metric, distribution);
    }

    private void reportHistogram(final Metric m, final Snapshot s) {
        send(m.attribute("stat", "min").value(s.getMin()));
        send(m.attribute("stat", "max").value(s.getMax()));
        send(m.attribute("stat", "mean").value(s.getMean()));
        send(m.attribute("stat", "median").value(s.getMedian()));
        send(m.attribute("stat", "stddev").value(s.getStdDev()));
        reportHistogramQuantiles(m, s);
    }

    private void reportHistogramQuantiles(final Metric m, final Snapshot s) {
        for (Percentile q : histogramPercentiles) {
            send(m.attribute("stat", q.getPercentileString()).value(s.getValue(q.getQuantile())));
        }
    }

    private void reportMetered(final Metric m, Metered value) {
        final String u = getUnit(m);
        final Metric r = m.attribute("unit", u + "/s");
        send(r.attribute("stat", "1m").value(value.getOneMinuteRate()));
        send(r.attribute("stat", "5m").value(value.getFiveMinuteRate()));
    }

    private void reportDistribution(final com.spotify.ffwd.v1.Metric metric,
                                    final Distribution distribution) {
        ByteString byteString = distribution.getValueAndFlush();
        Value value = Value.distributionValue(byteString);
        send(metric.value(value));
    }

    private String getUnit(final Metric m) {
        final String unit = m.getAttributes().get("unit");

        if (unit == null) {
            return "n";
        }

        return unit;
    }

    private void send(Metric metric) {
        final Map<String, String> tags = tagExtractor.addTags(metric.getAttributes());
        final Metric taggedMetric = metric.attributes(tags);

        try {
            client.send(taggedMetric);
        } catch (IOException e) {
            log.error("Failed to send metric", e);
        }
    }

    private void send(com.spotify.ffwd.v1.Metric metric) {
        final Map<String, String> tags = tagExtractor.addTags(metric.getAttributes());
        final com.spotify.ffwd.v1.Metric taggedMetric = metric.attributes(tags);

        try {
            client.send(taggedMetric);
        } catch (IOException e) {
            log.error("Failed to send metric", e);
        }
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }

        scheduledFuture = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    FastForwardReporter.this.report();
                } catch (final Exception e) {
                    log.error("Error when trying to report metric", e);
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

    public void stopWithFlush() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        if (executorOwner) {
            executorService.shutdown();
        }
        try {
            log.info("Final flush of metrics.");
            report();
        } catch (final Exception e) {
            log.error("Error during final flush of metrics: ", e);
        }
    }

    @Override
    public void close() {
        stop();
    }
}
