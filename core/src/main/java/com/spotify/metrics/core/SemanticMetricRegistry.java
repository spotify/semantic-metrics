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

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Timer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * A registry of metric instances.
 */
public class SemanticMetricRegistry implements SemanticMetricSet {
    private final ConcurrentMap<MetricId, Metric> metrics;
    private final List<SemanticMetricRegistryListener> listeners;
    private final Supplier<Reservoir> defaultReservoirSupplier;

    /**
     * Creates a new {@link SemanticMetricRegistry}.
     */
    public SemanticMetricRegistry(final ConcurrentMap<MetricId, Metric> metrics) {
        this(metrics, () -> new ExponentiallyDecayingReservoir());
    }

    /**
     * Creates a new {@link SemanticMetricRegistry}.
     */
    public SemanticMetricRegistry() {
        // This is only for backward compatibility purpose. After removing the "buildMap" method
        // we should call this(new ConcurrentHashMap<MetricId, Metric>()) instead.
        this(new ConcurrentHashMap<MetricId, Metric>(), () -> new ExponentiallyDecayingReservoir());
    }

    public SemanticMetricRegistry(final Supplier<Reservoir> defaultReservoirSupplier) {
        this.metrics = new ConcurrentHashMap<MetricId, Metric>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.defaultReservoirSupplier = defaultReservoirSupplier;
    }

    public SemanticMetricRegistry(
        final ConcurrentMap<MetricId, Metric> metrics,
        final Supplier<Reservoir> defaultReservoirSupplier
    ) {
        this.metrics = metrics;
        this.listeners = new CopyOnWriteArrayList<>();
        this.defaultReservoirSupplier = defaultReservoirSupplier;
    }

    /**
     * Creates a new {@link ConcurrentMap} implementation for use inside the registry. Override this
     * to create a {@link SemanticMetricRegistry} with space- or time-bounded metric lifecycles, for
     * example.
     *
     * @return a new {@link ConcurrentMap}
     */
    @Deprecated
    // http://stackoverflow.com/questions/3404301/whats-wrong-with-overridable-method-calls-in
    // -constructors
    protected ConcurrentMap<MetricId, Metric> buildMap() {
        return new ConcurrentHashMap<MetricId, Metric>();
    }

    /**
     * Given a {@link Metric}, registers it under the given name.
     *
     * @param name   the name of the metric
     * @param metric the metric
     * @param <T>    the type of the metric
     * @return {@code metric}
     * @throws IllegalArgumentException if the name is already registered
     */
    public <T extends Metric> T register(@NonNull final MetricId name, @NonNull final T metric)
        throws IllegalArgumentException {
        if (metric == null) {
            throw new IllegalArgumentException("A metric cannot be null");
        }
        if (metric instanceof SemanticMetricSet) {
            registerAll(name, (SemanticMetricSet) metric);
        } else {
            final Metric existing = metrics.putIfAbsent(name, metric);
            if (existing == null) {
                onMetricAdded(name, metric);
            } else {
                throw new IllegalArgumentException("A metric named " + name + " already exists");
            }
        }
        return metric;
    }

    /**
     * Given a metric set, registers them.
     *
     * @param metrics a set of metrics
     * @throws IllegalArgumentException if any of the names are already registered
     */
    public void registerAll(final SemanticMetricSet metrics) throws IllegalArgumentException {
        registerAll(MetricId.EMPTY, metrics);
    }

    /**
     * Creates a new {@link Counter} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link Counter}
     */
    public Counter counter(final MetricId name) {
        return getOrAdd(name, SemanticMetricBuilder.COUNTERS);
    }

    /**
     * Creates a new {@link Histogram} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link Histogram}
     */
    public Histogram histogram(final MetricId name) {
        return getOrAdd(name,
            SemanticMetricBuilderFactory.histogramWithReservoir(defaultReservoirSupplier));
    }

    /**
     * Creates a new {@link Distribution} or return an existing one registers under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link Distribution}
     */
    public Distribution distribution(final MetricId name) {
        return getOrAdd(name, SemanticMetricBuilder.DISTRIBUTION);
    }

    /**
     * Creates a new {@link Histogram} with a custom {@link Reservoir} and registers it under
     * the given name.
     *
     * @param name              the name of the metric
     * @param reservoirSupplier a {@link Supplier} that returns an instance of {@link Reservoir}
     * @return a new {@link Histogram}
     */
    public Histogram histogram(final MetricId name, Supplier<Reservoir> reservoirSupplier) {
        return getOrAdd(name,
            SemanticMetricBuilderFactory.histogramWithReservoir(reservoirSupplier));
    }

    /**
     * Creates a new {@link Meter}  and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link Meter}
     */
    public Meter meter(final MetricId name) {
        return getOrAdd(name, SemanticMetricBuilder.METERS);
    }

    /**
     * Creates a new {@link Timer} and registers it under the given name.
     *
     * @param name the name of the metric
     * @return a new {@link Timer}
     */
    public Timer timer(final MetricId name) {
        return getOrAdd(name,
            SemanticMetricBuilderFactory.timerWithReservoir(defaultReservoirSupplier));
    }


    /**
     * Creates a new {@link Timer} with a custom {@link Reservoir} and registers it under the given
     * name.
     *
     * @param name              the name of the metric
     * @param reservoirSupplier a {@link Supplier} that returns an instance of {@link Reservoir}
     * @return a new {@link Timer}
     */
    public Timer timer(final MetricId name, Supplier<Reservoir> reservoirSupplier) {
        return getOrAdd(name, SemanticMetricBuilderFactory.timerWithReservoir(reservoirSupplier));
    }

    public DerivingMeter derivingMeter(final MetricId name) {
        return getOrAdd(name, SemanticMetricBuilder.DERIVING_METERS);
    }

    /**
     * Removes the metric with the given name.
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    public boolean remove(final MetricId name) {
        final Metric metric = metrics.remove(name);
        if (metric != null) {
            onMetricRemoved(name, metric);
            return true;
        }
        return false;
    }

    /**
     * Removes all metrics which match the given filter.
     *
     * @param filter a filter
     */
    public void removeMatching(final SemanticMetricFilter filter) {
        for (final Map.Entry<MetricId, Metric> entry : metrics.entrySet()) {
            if (filter.matches(entry.getKey(), entry.getValue())) {
                remove(entry.getKey());
            }
        }
    }

    /**
     * Adds a {@link SemanticMetricRegistryListener} to a collection of listeners that will be
     * notified on
     * metric creation.  Listeners will be notified in the order in which they are added.
     * <p/>
     * <b>N.B.:</b> The listener will be notified of all existing metrics when it first registers.
     *
     * @param listener the listener that will be notified
     */
    public void addListener(final SemanticMetricRegistryListener listener) {
        listeners.add(listener);

        for (final Map.Entry<MetricId, Metric> entry : metrics.entrySet()) {
            notifyListenerOfAddedMetric(listener, entry.getValue(), entry.getKey());
        }
    }

    /**
     * Removes a {@link SemanticMetricRegistryListener} from this registry's collection of
     * listeners.
     *
     * @param listener the listener that will be removed
     */
    public void removeListener(final SemanticMetricRegistryListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns a set of the names of all the metrics in the registry.
     *
     * @return the names of all the metrics
     */
    public SortedSet<MetricId> getNames() {
        return Collections.unmodifiableSortedSet(new TreeSet<MetricId>(metrics.keySet()));
    }

    /**
     * Returns a map of all the gauges in the registry and their names.
     *
     * @return all the gauges in the registry
     */
    @SuppressWarnings("rawtypes")
    public SortedMap<MetricId, Gauge> getGauges() {
        return getGauges(SemanticMetricFilter.ALL);
    }

    /**
     * Returns a map of all the gauges in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the gauges in the registry
     */
    @SuppressWarnings("rawtypes")
    public SortedMap<MetricId, Gauge> getGauges(final SemanticMetricFilter filter) {
        return getMetrics(Gauge.class, filter);
    }

    /**
     * Returns a map of all the counters in the registry and their names.
     *
     * @return all the counters in the registry
     */
    public SortedMap<MetricId, Counter> getCounters() {
        return getCounters(SemanticMetricFilter.ALL);
    }

    /**
     * Returns a map of all the counters in the registry and their names which match the given
     * filter.
     *
     * @param filter the metric filter to match
     * @return all the counters in the registry
     */
    public SortedMap<MetricId, Counter> getCounters(final SemanticMetricFilter filter) {
        return getMetrics(Counter.class, filter);
    }

    /**
     * Returns a map of all the histograms in the registry and their names.
     *
     * @return all the histograms in the registry
     */
    public SortedMap<MetricId, Histogram> getHistograms() {
        return getHistograms(SemanticMetricFilter.ALL);
    }

    /**
     * Returns a map of all the histograms in the registry and their names which match the given
     * filter.
     *
     * @param filter the metric filter to match
     * @return all the histograms in the registry
     */
    public SortedMap<MetricId, Histogram> getHistograms(final SemanticMetricFilter filter) {
        return getMetrics(Histogram.class, filter);
    }

    /**
     * Returns a map of all the meters in the registry and their names.
     *
     * @return all the meters in the registry
     */
    public SortedMap<MetricId, Meter> getMeters() {
        return getMeters(SemanticMetricFilter.ALL);
    }

    /**
     * Returns a map of all the meters in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the meters in the registry
     */
    public SortedMap<MetricId, Meter> getMeters(final SemanticMetricFilter filter) {
        return getMetrics(Meter.class, filter);
    }

    /**
     * Returns a map of all the timers in the registry and their names.
     *
     * @return all the timers in the registry
     */
    public SortedMap<MetricId, Timer> getTimers() {
        return getTimers(SemanticMetricFilter.ALL);
    }

    /**
     * Returns a map of all the timers in the registry and their names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return all the timers in the registry
     */
    public SortedMap<MetricId, Timer> getTimers(final SemanticMetricFilter filter) {
        return getMetrics(Timer.class, filter);
    }

    /**
     * Returns a map of all the deriving meters in the registry and their names which match the
     * given filter.
     *
     * @param filter the metric filter to match
     * @return all the deriving meters in the registry
     */
    public SortedMap<MetricId, DerivingMeter> getDerivingMeters(final SemanticMetricFilter filter) {
        return getMetrics(DerivingMeter.class, filter);
    }

    /**
     * Returns a map of all the distributions metrics in the registry and their
     * names which match the given filter.
     *
     * @param filter the metric filter to match
     * @return a sorted Map of distribution metrics
     */
    public SortedMap<MetricId, Distribution> getDistributions(final SemanticMetricFilter filter) {
        return getMetrics(Distribution.class, filter);
    }


    /**
     * Atomically adds the given metric to the set of metrics.
     * <p>
     * A side effect of this method is the calling of {@link #onMetricAdded(MetricId, Metric)} if
     * a new
     * metric is added.
     * <p>
     * This method should only be used on non-{@code SemanticMetricSet} metrics.
     *
     * @param name   Name of the metric to atomically add if absent.
     * @param metric The metric to atomically add if absent.
     * @return {@code null} if the metric was added, or the previously mapped metric.
     */
    protected Metric addIfAbsent(final MetricId name, final Metric metric) {
        final Metric previous = metrics.putIfAbsent(name, metric);

        if (previous == null) {
            onMetricAdded(name, metric);
            return null;
        }

        return previous;
    }

    @SuppressWarnings("unchecked")
    public <T extends Metric> T getOrAdd(
        final MetricId name, final SemanticMetricBuilder<T> builder
    ) {
        final Metric metric = metrics.get(name);

        if (metric != null) {
            if (!builder.isInstance(metric)) {
                throw new IllegalArgumentException(
                    name + " is already used for a different type of metric");
            }

            return (T) metric;
        }

        final T addition = builder.newMetric();

        if (addition instanceof SemanticMetricSet) {
            // XXX: getOrAdd has really bad behaviour in supporting both Metric, and
            // SemanticMetricSet,
            //      it effectively causes the method to behave in two different ways depending on
            // the type
            //      of the created metric.
            //      solution: implement getOrAddSet specifically for adding a set of metrics.
            registerAll(name, (SemanticMetricSet) addition);
            return addition;
        }

        final Metric previous = addIfAbsent(name, addition);

        if (previous == null) {
            return addition;
        }

        if (!builder.isInstance(previous)) {
            throw new IllegalArgumentException(
                name + " is already used for a different type of metric");
        }

        return (T) previous;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Metric> SortedMap<MetricId, T> getMetrics(
        final Class<T> klass, final SemanticMetricFilter filter
    ) {
        final TreeMap<MetricId, T> metrics = new TreeMap<MetricId, T>();

        for (final Map.Entry<MetricId, Metric> entry : this.metrics.entrySet()) {
            if (klass.isInstance(entry.getValue()) &&
                filter.matches(entry.getKey(), entry.getValue())) {
                metrics.put(entry.getKey(), (T) entry.getValue());
            }
        }

        return Collections.unmodifiableSortedMap(metrics);
    }

    protected void onMetricAdded(final MetricId name, final Metric metric) {
        for (final SemanticMetricRegistryListener listener : listeners) {
            notifyListenerOfAddedMetric(listener, metric, name);
        }
    }

    private void notifyListenerOfAddedMetric(
        final SemanticMetricRegistryListener listener, final Metric metric, final MetricId name
    ) {
        if (metric instanceof Gauge) {
            listener.onGaugeAdded(name, (Gauge<?>) metric);
        } else if (metric instanceof Counter) {
            listener.onCounterAdded(name, (Counter) metric);
        } else if (metric instanceof Histogram) {
            listener.onHistogramAdded(name, (Histogram) metric);
        } else if (metric instanceof Meter) {
            listener.onMeterAdded(name, (Meter) metric);
        } else if (metric instanceof Timer) {
            listener.onTimerAdded(name, (Timer) metric);
        } else if (metric instanceof DerivingMeter) {
            listener.onDerivingMeterAdded(name, (DerivingMeter) metric);
        } else if (metric instanceof Distribution) {
            listener.onDistributionAdded(name, (Distribution) metric);
        } else {
            throw new IllegalArgumentException("Unknown metric type: " + metric.getClass());
        }
    }


    protected void onMetricRemoved(final MetricId name, final Metric metric) {
        for (final SemanticMetricRegistryListener listener : listeners) {
            notifyListenerOfRemovedMetric(name, metric, listener);
        }
    }

    private void notifyListenerOfRemovedMetric(
        final MetricId name, final Metric metric, final SemanticMetricRegistryListener listener
    ) {
        if (metric instanceof Gauge) {
            listener.onGaugeRemoved(name);
        } else if (metric instanceof Counter) {
            listener.onCounterRemoved(name);
        } else if (metric instanceof Histogram) {
            listener.onHistogramRemoved(name);
        } else if (metric instanceof Meter) {
            listener.onMeterRemoved(name);
        } else if (metric instanceof Timer) {
            listener.onTimerRemoved(name);
        } else if (metric instanceof DerivingMeter) {
            listener.onDerivingMeterRemoved(name);
        } else if (metric instanceof Distribution) {
            listener.onDistributionRemoved(name);
        } else {
            throw new IllegalArgumentException("Unknown metric type: " + metric.getClass());
        }
    }

    protected void registerAll(final MetricId prefix, final SemanticMetricSet metrics)
        throws IllegalArgumentException {
        for (final Map.Entry<MetricId, Metric> entry : metrics.getMetrics().entrySet()) {
            if (entry.getValue() instanceof SemanticMetricSet) {
                registerAll(MetricId.join(prefix, entry.getKey()),
                    (SemanticMetricSet) entry.getValue());
            } else {
                register(MetricId.join(prefix, entry.getKey()), entry.getValue());
            }
        }
    }

    @Override
    public Map<MetricId, Metric> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }
}
