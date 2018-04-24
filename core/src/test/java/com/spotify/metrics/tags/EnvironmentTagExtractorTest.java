package com.spotify.metrics.tags;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentTagExtractorTest {

    private Map<String, String> tags;

    @Before
    public void setUp() {
        tags = ImmutableMap.of("FFWD_TAG_foo", "bar",
            "PATH", "ignored:ignored", "FFWD_TAG_bar", "baz");
    }

    /**
     * Test that a map containing environment variables correctly extracts and discards variables
     * changing the tags in ffwd and retains the present ffwd tags.
     */
    @Test
    public void testAddTags() {
        final Supplier<Map<String, String>> environmentSupplier =
            Suppliers.ofInstance(tags);

        final EnvironmentTagExtractor environmentTagExtractor =
            new EnvironmentTagExtractor(environmentSupplier);

        final Map<String, String> currentMap = ImmutableMap.of("cluster", "cluster1");

        final Map<String, String> out = environmentTagExtractor.addTags(currentMap);

        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz", "cluster", "cluster1"), out);
    }

    /**
     * Test that a map containing environment variables correctly extracts and discards variables
     * changing the tags in ffwd.
     */
    @Test
    public void testFilterEnvironment() {
        final Map<String, String> out = EnvironmentTagExtractor.filterEnvironmentTags(tags);
        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz"), out);
    }
}
