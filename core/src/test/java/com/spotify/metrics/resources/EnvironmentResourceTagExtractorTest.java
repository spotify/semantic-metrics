package com.spotify.metrics.resources;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentResourceTagExtractorTest {

    private Map<String, String> resources;

    @Before
    public void setUp() {
        resources = ImmutableMap.of("FFWD_RESOURCE_foo", "bar",
            "PATH", "ignored:ignored", "FFWD_RESOURCE_bar", "baz");
    }

    /**
     * Test that a map containing environment variables correctly extracts and discards variables
     * changing the resources in ffwd and retains the present ffwd resources.
     */
    @Test
    public void testAddResources() {
        final Supplier<Map<String, String>> environmentSupplier =
            Suppliers.ofInstance(resources);

        final EnvironmentResourceTagExtractor environmentResourceTagExtractor =
            new EnvironmentResourceTagExtractor(environmentSupplier);

        final Map<String, String> currentMap = ImmutableMap.of("cluster", "cluster1");

        final Map<String, String> out = environmentResourceTagExtractor.addResources(currentMap);

        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz", "cluster", "cluster1"), out);
    }

    /**
     * Test that a map containing environment variables correctly extracts and discards variables
     * changing the resources in ffwd.
     */
    @Test
    public void testFilterEnvironment() {
        final Map<String, String> out =
         EnvironmentResourceTagExtractor.filterEnvironmentResources(resources);
        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz"), out);
    }

}
