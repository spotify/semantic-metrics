package com.spotify.metrics.tags;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;

public class NoopTagExtractorTest {
    /**
     * Test that no tags are extracted and added.
     */
    @Test
    public void testAddTags() {
        final NoopTagExtractor noopTagExtractor = new NoopTagExtractor();

        final Map<String, String> currentMap = ImmutableMap.of("foo", "bar",
            "bar", "baz", "cluster", "cluster1");

        final Map<String, String> out = noopTagExtractor.addTags(currentMap);

        assertEquals(ImmutableMap.of("foo", "bar", "bar", "baz", "cluster", "cluster1"), out);
    }
}
