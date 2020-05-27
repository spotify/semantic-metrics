package com.spotify.metrics.jmh;

import com.codahale.metrics.Snapshot;
import com.spotify.metrics.core.HistogramWithTtl;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * The benchmark here tries to simulate real-world usage of a Histogram, where several threads are
 * calling update() and one thread is calling getSnapshot (the ffwd reporting thread). This is
 * achieved by having two Benchmark methods in the same Group - JMH runs both concurrently.
 *
 * TODO: In the real world, a service with a lot of request volume, `histogram.update()` will be called
 * a lot more frequently than `histogram.getSnapshot()` - need to look into how to model this with
 * JMH.
 * The number of threads used for each method defaults to a 10:1 ratio (via the annotations on the
 * methods), but this can be overridden when running the benchmark with the `-tg` flag.
 *
 * Reference: https://hg.openjdk.java.net/code-tools/jmh/file/b6f87aa2a687/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_15_Asymmetric.java
 */
@State(Scope.Group)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class OriginalReservoirWithTtlBenchmark {

  private HistogramWithTtl histogram;
  private OriginalHistogramWithTtl originalHistogram;

  @Setup
  public void setUp() {
    histogram = new HistogramWithTtl();
    originalHistogram = new OriginalHistogramWithTtl();
  }

  @Benchmark
  @Group("new_updateAndSnapshot")
  @GroupThreads(100)
  public void update() {
    histogram.update(42);
  }

  @Benchmark
  @Group("new_updateAndSnapshot")
  @GroupThreads(1)
  public Snapshot getSnapshot() {
    return histogram.getSnapshot();
  }

  @Benchmark
  @Group("original_updateAndSnapshot")
  @GroupThreads(100)
  public void originalUpdate() {
    originalHistogram.update(42);
  }

  @Benchmark
  @Group("original_updateAndSnapshot")
  @GroupThreads(1)
  public Snapshot originalGetSnapshot() {
    return originalHistogram.getSnapshot();
  }

//  public static void main(String[] args) {
//
//  }
}
