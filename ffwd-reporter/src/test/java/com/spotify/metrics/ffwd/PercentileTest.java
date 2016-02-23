package com.spotify.metrics.ffwd;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class PercentileTest {

    @Test
    public void testPercentilesSet() {
        Set<Percentile> benchmark =
            Sets.newHashSet(new Percentile(75), new Percentile(99), new Percentile(90, 7),
                new Percentile(99, 1), new Percentile(90, 1));

        Set<Percentile> set = Sets.newHashSet(new Percentile(75), new Percentile(99));
        set.add(new Percentile(75));
        set.add(new Percentile(90, 7));
        set.add(new Percentile(90, 7));
        set.add(new Percentile(99, 1));
        set.add(new Percentile(90, 1));
        set.add(new Percentile(99));

        Assert.assertEquals(benchmark, set);
    }

    @Test
    public void testPercentileQuantiles() {
        Assert.assertEquals(new Percentile(75).getQuantile(), 0.75, 0);
        Assert.assertEquals(new Percentile(90, 1).getQuantile(), 0.901, 0);
        Assert.assertEquals(new Percentile(90, 7).getQuantile(), 0.907, 0);
        Assert.assertEquals(new Percentile(99, 1).getQuantile(), 0.991, 0);
        Assert.assertEquals(new Percentile(99).getQuantile(), 0.99, 0);
        Assert.assertEquals(new Percentile(7, 2).getQuantile(), 0.072, 0);

        Assert.assertEquals(new Percentile(75).getPercentileString(), "p75");
        Assert.assertEquals(new Percentile(90, 1).getPercentileString(), "p90.1");
        Assert.assertEquals(new Percentile(90, 7).getPercentileString(), "p90.7");
        Assert.assertEquals(new Percentile(99, 1).getPercentileString(), "p99.1");
        Assert.assertEquals(new Percentile(99).getPercentileString(), "p99");
        Assert.assertEquals(new Percentile(7, 2).getPercentileString(), "p7.2");
    }

    @Test
    public void testConstructingPercentiles() {
        Assert.assertEquals(new Percentile(75), new Percentile(0.75));
        Assert.assertEquals(new Percentile(10, 1), new Percentile(0.101));
        Assert.assertEquals(new Percentile(0, 8), new Percentile(0.008));
        Assert.assertEquals(new Percentile(9), new Percentile(0.09));
        Assert.assertEquals(new Percentile(0), new Percentile(0.0));
    }

    @Test
    public void testIllegalPercentiles() {
        checkIllegalPercentile(-1, 0);
        checkIllegalPercentile(100, 0);
        checkIllegalPercentile(110, 1);
        checkIllegalPercentile(120, 2);
        checkIllegalPercentile(90, -1);
        checkIllegalPercentile(90, 10);
        checkIllegalPercentile(98, 12);

        checkIllegalPercentile(1.1);
        checkIllegalPercentile(-0.1);
        checkIllegalPercentile(1.0);
    }

    private void checkIllegalPercentile(double quantile) {
        try {
            new Percentile(quantile);
            Assert.fail("Illegal argument is accepted by Percentile: " + quantile);
        } catch (IllegalArgumentException e) {
        }
    }

    private void checkIllegalPercentile(int whole, int decimal) {
        try {
            new Percentile(whole, decimal);
            Assert.fail("Illegal argument is accepted by Percentile: " + whole + ", " + decimal);
        } catch (IllegalArgumentException e) {
        }
    }
}
