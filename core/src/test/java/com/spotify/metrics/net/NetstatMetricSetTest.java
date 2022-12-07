package com.spotify.metrics.net;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.Gauge;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.spotify.metrics.core.DerivedLongGauge;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricFilter;
import com.spotify.metrics.core.SemanticMetricRegistry;
import java.io.File;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NetstatMetricSetTest {

    private SemanticMetricRegistry registry;

    @Before
    public void setUp() {
        registry = new SemanticMetricRegistry();
    }

    @Test
    public void testParseProc() {
        NetstatMetricSet netstatMetricSet = new NetstatMetricSet(
            () -> NetstatMetricSet.readData(absolutePath("proc/net/netstat"),
                absolutePath("proc/net/snmp")));

        registry.registerAll(netstatMetricSet);
        assertEquals(15170L, valueFrom(registry.getGauges(filter("segments-retransmitted"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("bad-segments-received"))));
        assertEquals(2677L, valueFrom(registry.getGauges(filter("tcp-timeouts"))));
        assertEquals(2055L, valueFrom(registry.getGauges(filter("tcp-lost-retransmit"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-syn-retrans"))));
    }

    @Test
    public void testHandleFileNotFound() {
        NetstatMetricSet netstatMetricSet = new NetstatMetricSet(
            () -> NetstatMetricSet.readData("/tmp/NetstatMetricSetTest.testHandleFileNotFound",
                "/tmp/NetstatMetricSetTest.testHandleFileNotFound"));
        registry.registerAll(netstatMetricSet);
        assertEquals(0L, valueFrom(registry.getGauges(filter("segments-retransmitted"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("bad-segments-received"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-timeouts"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-lost-retransmit"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-syn-retrans"))));
    }

    @Test
    public void testMalformedNetstatFile() {
        NetstatMetricSet netstatMetricSet = new NetstatMetricSet(
            () -> NetstatMetricSet.readData(absolutePath("proc/net/netstat.invalid"),
                absolutePath("proc/net/snmp")));

        registry.registerAll(netstatMetricSet);
        assertEquals(15170L, valueFrom(registry.getGauges(filter("segments-retransmitted"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("bad-segments-received"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-timeouts"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-lost-retransmit"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-syn-retrans"))));
    }

    @Test
    public void testMalformedSnmpFileValues() {
        NetstatMetricSet netstatMetricSet = new NetstatMetricSet(
            () -> NetstatMetricSet.readData(absolutePath("proc/net/netstat"),
                absolutePath("proc/net/snmp.invalid")));

        registry.registerAll(netstatMetricSet);
        assertEquals(0L, valueFrom(registry.getGauges(filter("segments-retransmitted"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("bad-segments-received"))));
        assertEquals(2677L, valueFrom(registry.getGauges(filter("tcp-timeouts"))));
        assertEquals(2055L, valueFrom(registry.getGauges(filter("tcp-lost-retransmit"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("tcp-syn-retrans"))));
    }

    @Test
    public void testMemoizingSupplier() {
        AtomicInteger supplyCounter = new AtomicInteger();
        Supplier<NetstatMetricSet.Data> dataSupplier = Suppliers.memoizeWithExpiration(() -> {
            supplyCounter.incrementAndGet();
            return new NetstatMetricSet.Data();
        }, 10, TimeUnit.SECONDS);

        NetstatMetricSet netstatMetricSet = new NetstatMetricSet(dataSupplier);

        registry.registerAll(netstatMetricSet);
        assertEquals(0L, valueFrom(registry.getGauges(filter("segments-retransmitted"))));
        assertEquals(0L, valueFrom(registry.getGauges(filter("bad-segments-received"))));
        assertEquals(1, supplyCounter.get());
    }

    private SemanticMetricFilter filter(final String what) {
        return (metricId, metric) -> metricId.getTags().get("what").equals(what);
    }

    private long valueFrom(final SortedMap<MetricId, Gauge> map) {
        Optional<Gauge> first = map.values().stream().findFirst();
        DerivedLongGauge gauge = (DerivedLongGauge) first.get();
        return gauge.getNext();
    }

    private String absolutePath(String name) {
        File netStat = new File(getClass().getClassLoader().getResource(name).getFile());
        return netStat.getAbsolutePath();
    }
}
