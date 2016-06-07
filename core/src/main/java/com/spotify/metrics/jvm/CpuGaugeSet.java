package com.spotify.metrics.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of gauges reporting JVM CPU usage statistics.
 */
public class CpuGaugeSet implements SemanticMetricSet {

  private final OperatingSystemMXBean operatingSystemMXBean;

  private CpuGaugeSet(OperatingSystemMXBean operatingSystemMXBean) {
    this.operatingSystemMXBean = operatingSystemMXBean;
  }

  @Override
  public Map<MetricId, Metric> getMetrics() {
    if (!(operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean)) {
      return Collections.emptyMap();
    }

    final com.sun.management.OperatingSystemMXBean osMxBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;

    final Map<MetricId, Metric> gauges = new HashMap<>();
    final MetricId cpu = MetricId.build().tagged("what", "jvm-cpu-stats");

    gauges.put(cpu.tagged("what", "process-cpu-load-percentage", "unit", "%"), new Gauge<Double>() {
      @Override
      public Double getValue() {
        return osMxBean.getProcessCpuLoad();
      }
    });

    gauges.put(cpu.tagged("what", "system-cpu-load-percentage", "unit", "%"), new Gauge<Double>() {
      @Override
      public Double getValue() {
        return osMxBean.getSystemCpuLoad();
      }
    });

    gauges.put(cpu.tagged("what", "system-load-average"), new Gauge<Double>() {
      @Override
      public Double getValue() {
        return osMxBean.getSystemLoadAverage();
      }
    });

    gauges.put(cpu.tagged("what", "process-cpu-time", "unit", "ns"), new Gauge<Long>() {
      @Override
      public Long getValue() {
        return osMxBean.getProcessCpuTime();
      }
    });

    return Collections.unmodifiableMap(gauges);
  }

  public static CpuGaugeSet create() {
    return new CpuGaugeSet(ManagementFactory.getOperatingSystemMXBean());
  }
}
