package com.spotify.metrics.resources;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;

public class NoopResourceTagExtractorTest {

    /**
     * Test that no resources are extracted and added.
     */
    @Test
    public void testAddResources() {
        final NoopResourceTagExtractor noopResourceTagExtractor = new NoopResourceTagExtractor();

        final Map<String, String> currentMap = ImmutableMap.of("foo", "bar",
            "bar", "baz", "cluster", "cluster1");

        final Map<String, String> out = noopResourceTagExtractor.addResources(currentMap);

        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz", "cluster", "cluster1"), out);
    }

}
