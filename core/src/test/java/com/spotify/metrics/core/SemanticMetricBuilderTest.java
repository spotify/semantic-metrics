package com.spotify.metrics.core;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class SemanticMetricBuilderTest {
    @Test
    public void testDistributionBuilder(){
        Distribution distribution = SemanticMetricBuilder.DISTRIBUTION.newMetric();
        assertTrue(SemanticMetricBuilder.DISTRIBUTION.isInstance(distribution));
    }

}
