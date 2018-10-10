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

/*
 * This class was initially copied and modified from the codahale metrics project.
 *
 * For the appropriate license (there was no header) see LICENSE.codahale.txt
 *
 * It was copied from the following tree:
 * https://github.com/dropwizard/metrics/tree/6d1fff844b7fc8855b81bb42b7125bd84f3a3e7a
 */
package com.spotify.metrics.core;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is an adaptation of Dropwizard's InstrumentedScheduledExecutorService that uses {@link
 * com.spotify.metrics.core.SemanticMetricRegistry}.
 * <p>
 * Just as Dropwizard's, this class wraps an {@link java.util.concurrent.ScheduledExecutorService}
 * and monitors the number of tasks submitted, running, completed and keeps a {@link
 * com.codahale.metrics.Timer} to track task duration.
 * <p>
 * It will register the metrics with the provided base metric ID and a "what" tag to identify the
 * metric object, e.g: "submitted", "running", etc. If no basic metric ID is provided a base metric
 * with tag "executor" set to a sequential "scheduled-executor-N" value is used.
 */
public class InstrumentedScheduledExecutorService implements ScheduledExecutorService {

    private static final AtomicLong NAME_COUNTER = new AtomicLong();

    private final ScheduledExecutorService delegate;

    private final Meter submitted;
    private final Counter running;
    private final Meter completed;
    private final Timer duration;

    private final Meter scheduledOnce;
    private final Meter scheduledRepetitively;
    private final Counter scheduledOverrun;
    private final Histogram percentOfPeriod;

    /**
     * Wraps an {@link java.util.concurrent.ScheduledExecutorService} uses an auto-generated default
     * name.
     *
     * @param delegate {@link java.util.concurrent.ScheduledExecutorService} to wrap.
     * @param registry {@link com.codahale.metrics.MetricRegistry} that will contain the metrics.
     */
    public InstrumentedScheduledExecutorService(
        ScheduledExecutorService delegate, SemanticMetricRegistry registry
    ) {
        this(delegate, registry, new MetricId().tagged("executor",
            "scheduled-executor-" + NAME_COUNTER.incrementAndGet()));
    }

    /**
     * Wraps an {@link java.util.concurrent.ScheduledExecutorService} with an explicit name.
     *
     * @param delegate {@link java.util.concurrent.ScheduledExecutorService} to wrap.
     * @param registry {@link SemanticMetricRegistry} that will contain the metrics.
     * @param baseMetricId base metric id for this executor service.
     */
    public InstrumentedScheduledExecutorService(
        ScheduledExecutorService delegate, SemanticMetricRegistry registry, MetricId baseMetricId
    ) {
        this.delegate = delegate;
        MetricId baseMetricIdWithUnit = baseMetricId.tagged("unit", "task");
        this.submitted = registry.meter(baseMetricIdWithUnit.tagged("what", "submitted"));
        this.running = registry.counter(baseMetricIdWithUnit.tagged("what", "running"));
        this.completed = registry.meter(baseMetricIdWithUnit.tagged("what", "completed"));
        this.duration = registry.timer(baseMetricIdWithUnit.tagged("what", "duration"));

        this.scheduledOnce = registry.meter(baseMetricIdWithUnit.tagged("what", "scheduled.once"));
        this.scheduledRepetitively =
            registry.meter(baseMetricIdWithUnit.tagged("what", "scheduled.repetitively"));
        this.scheduledOverrun =
            registry.counter(baseMetricIdWithUnit.tagged("what", "scheduled.overrun"));
        this.percentOfPeriod =
            registry.histogram(baseMetricIdWithUnit.tagged("what", "scheduled.percent-of-period"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        scheduledOnce.mark();
        return delegate.schedule(new InstrumentedRunnable(command), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        scheduledOnce.mark();
        return delegate.schedule(new InstrumentedCallable<>(callable), delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
        Runnable command, long initialDelay, long period, TimeUnit unit
    ) {
        scheduledRepetitively.mark();
        return delegate.scheduleAtFixedRate(new InstrumentedPeriodicRunnable(command, period, unit),
            initialDelay, period, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit
    ) {
        scheduledRepetitively.mark();
        return delegate.scheduleWithFixedDelay(new InstrumentedRunnable(command), initialDelay,
            delay, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitted.mark();
        return delegate.submit(new InstrumentedCallable<>(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        submitted.mark();
        return delegate.submit(new InstrumentedRunnable(task), result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(Runnable task) {
        submitted.mark();
        return delegate.submit(new InstrumentedRunnable(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit
    ) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented, timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> instrument(
        Collection<? extends Callable<T>> tasks
    ) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumented.add(new InstrumentedCallable<>(task));
        }
        return instrumented;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable command) {
        submitted.mark();
        delegate.execute(new InstrumentedRunnable(command));
    }

    private class InstrumentedRunnable implements Runnable {

        private final Runnable command;

        InstrumentedRunnable(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                command.run();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }

    private class InstrumentedPeriodicRunnable implements Runnable {

        private final Runnable command;
        private final long periodInNanos;

        InstrumentedPeriodicRunnable(Runnable command, long period, TimeUnit unit) {
            this.command = command;
            this.periodInNanos = unit.toNanos(period);
        }

        @Override
        public void run() {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                command.run();
            } finally {
                final long elapsed = context.stop();
                running.dec();
                completed.mark();
                if (elapsed > periodInNanos) {
                    scheduledOverrun.inc();
                }
                percentOfPeriod.update((100L * elapsed) / periodInNanos);
            }
        }
    }

    private class InstrumentedCallable<T> implements Callable<T> {

        private final Callable<T> task;

        InstrumentedCallable(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                return task.call();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }
}
