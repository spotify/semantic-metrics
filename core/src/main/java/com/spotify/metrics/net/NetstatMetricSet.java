package com.spotify.metrics.net;

import static com.google.common.base.Preconditions.checkNotNull;

import com.codahale.metrics.Metric;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.spotify.metrics.core.DerivedLongGauge;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NetstatMetricSet implements SemanticMetricSet {

    private static final String SPACES = "\\s+";
    private static final String PROC_SNMP_PATH = "/proc/net/snmp";
    private static final String PROC_NETSTAT_PATH = "/proc/net/netstat";

    private final Supplier<Data> dataSupplier;

    public NetstatMetricSet() {
        this(Suppliers.memoizeWithExpiration(NetstatMetricSet::readData, 10, TimeUnit.SECONDS));
    }

    @VisibleForTesting
    NetstatMetricSet(final Supplier<Data> dataSupplier) {
        this.dataSupplier = checkNotNull(dataSupplier, "dataSupplier");
    }

    @Override
    public Map<MetricId, Metric> getMetrics() {
        final Map<MetricId, Metric> gauges = new HashMap<>();
        gauges.put(
            MetricId.build().tagged("what", "segments-retransmitted", "unit", "tcp-segments/s"),
            new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return dataSupplier.get().segmentsRetransmitted;
                }
            });
        gauges.put(
            MetricId.build().tagged("what", "bad-segments-received", "unit", "tcp-segments/s"),
            new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return dataSupplier.get().badSegmentsReceived;
                }
            });
        gauges.put(MetricId.build().tagged("what", "tcp-timeouts", "unit", "timeouts/s"),
            new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return dataSupplier.get().tcpTimeouts;
                }
            });
        gauges.put(MetricId.build().tagged("what", "tcp-lost-retransmit", "unit", "retransmit/s"),
            new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return dataSupplier.get().tcpLostRetransmit;
                }
            });
        gauges.put(MetricId.build().tagged("what", "tcp-syn-retrans", "unit", "retransmit/s"),
            new DerivedLongGauge() {
                @Override
                public Long getNext() {
                    return dataSupplier.get().tcpSynRetrans;
                }
            });

        return gauges;
    }

    private static Data readData() {
        return readData(PROC_NETSTAT_PATH, PROC_SNMP_PATH);
    }

    @VisibleForTesting
    static Data readData(final String netstatPath, final String snmpPath) {
        final Data data = new Data();
        final Map<String, Long> netstatValues = readFile(netstatPath, "TcpExt:");
        final Map<String, Long> snmpValues = readFile(snmpPath, "Tcp:");

        data.badSegmentsReceived = snmpValues.getOrDefault("InErrs", 0L);
        data.segmentsRetransmitted = snmpValues.getOrDefault("RetransSegs", 0L);
        data.tcpLostRetransmit = netstatValues.getOrDefault("TCPLostRetransmit", 0L);
        data.tcpSynRetrans = netstatValues.getOrDefault("TCPSynRetrans", 0L);
        data.tcpTimeouts = netstatValues.getOrDefault("TCPTimeouts", 0L);

        return data;
    }

    private static Map<String, Long> readFile(final String fileName, final String prefix) {
        final Map<String, Long> kv = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] keys = line.trim().split(SPACES);
                line = br.readLine();
                String[] values = line.trim().split(SPACES);
                if (keys.length == values.length && keys.length > 0 &&
                    prefix.equals(keys[0].trim()) && prefix.equals(values[0].trim())) {
                    for (int i = 1; i < keys.length; i++) {
                        long value = Long.parseLong(values[i]);
                        kv.put(keys[i], value);
                    }
                }
            }
        } catch (Exception ex) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(kv);
    }

    static class Data {
        private long segmentsRetransmitted;
        private long badSegmentsReceived;
        private long tcpTimeouts;
        private long tcpLostRetransmit;
        private long tcpSynRetrans;
    }
}
