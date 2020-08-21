package com.spotify.metrics.core;

import com.spotify.metrics.core.codahale.metrics.ext.Distribution;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class SemanticMetricBuilderTest {
    @Test
    public void testDistributionBuilder(){
        Distribution distribution = SemanticMetricBuilder.DISTRIBUTION.newMetric();
        assertTrue(SemanticMetricBuilder.DISTRIBUTION.isInstance(distribution));
    }

}
