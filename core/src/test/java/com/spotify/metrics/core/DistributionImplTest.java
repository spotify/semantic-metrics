package com.spotify.metrics.core;

import com.tdunning.math.stats.TDigest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DistributionImplTest {

    private double [] period1Data = {1.34, 1.56, 1,58};
    private double [] period2Data  ={1.32, 1.57, 1.55, 0.456, 1,467, 2.45};

    private DistributionImpl distribution;

    @Before
    public void setup(){
        distribution = new DistributionImpl();
    }

    @Test
    public void testRecord(){
        Arrays.stream(period1Data).forEach(t->distribution.record(t));
        assertEquals(period1Data.length, distribution.getCount());
        Arrays.stream(period2Data).forEach(t->distribution.record(t));
        int expected = period1Data.length + period2Data.length;
        assertEquals(expected, distribution.getCount());
    }

    @Test
    public void testGetValueAndFlush(){
        Arrays.stream(period1Data).forEach(t->distribution.record(t));
        TDigest periode1tDigest = distribution.tDigest();
        distribution.getValueAndFlush();
        Arrays.stream(period1Data).forEach(t->distribution.record(t) );
        assertEquals(period1Data.length, distribution.getCount());
        TDigest periode2Digest = distribution.tDigest();
        assertNotEquals(periode1tDigest,periode2Digest);
    }

    @Test
    public void testRecordCountAfterGetValueAndFlush(){
        Arrays.stream(period1Data).forEach(t->distribution.record(t));
        distribution.getValueAndFlush();
        assertEquals(0, distribution.getCount());
    }



}
