package com.spotify.metrics.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class MetricIdTest {
    final String key = "key";

    @Test
    public void testEmpty() throws Exception {
        assertEquals(MetricId.EMPTY_TAGS, MetricId.EMPTY.getTags());
        assertEquals(null, MetricId.EMPTY.getKey());
        assertEquals(MetricId.EMPTY_TAGS, new MetricId().getTags());

        assertEquals(new MetricId(), MetricId.EMPTY);
        assertEquals(MetricId.EMPTY, MetricId.build());
        assertEquals(MetricId.EMPTY, MetricId.EMPTY.resolve(null));
    }

    @Test
    public void testEmptyResolve() throws Exception {
        final MetricId name = new MetricId();
        assertEquals(new MetricId("foo"), name.resolve("foo"));
    }

    @Test
    public void testResolveToEmpty() throws Exception {
        final MetricId name = new MetricId("foo");
        assertEquals(new MetricId("foo"), name.resolve(null));
    }

    @Test
    public void testResolve() throws Exception {
        final MetricId name = new MetricId("foo");
        assertEquals(new MetricId("foo.bar"), name.resolve("bar"));
    }

    @Test
    public void testResolveBothEmpty() throws Exception {
        final MetricId name = new MetricId(null);
        assertEquals(new MetricId(), name.resolve(null));
    }

    @Test
    public void testAddTagsVarious() {
        final Map<String, String> refTags = new HashMap<String, String>();
        refTags.put("foo", "bar");
        final MetricId test = MetricId.EMPTY.tagged("foo", "bar");
        final MetricId test2 = MetricId.EMPTY.tagged(refTags);

        assertEquals(new MetricId(null, refTags), test);
        assertEquals(refTags, test.getTags());

        assertEquals(new MetricId(null, refTags), test2);
        assertEquals(refTags, test2.getTags());
    }

    @Test
    public void testTaggedMoreArguments() {
        final Map<String, String> refTags = new HashMap<String, String>();
        refTags.put("foo", "bar");
        refTags.put("baz", "biz");
        assertEquals(refTags, MetricId.EMPTY.tagged("foo", "bar", "baz", "biz").getTags());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTaggedNotPairs() {
        MetricId.EMPTY.tagged("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTaggedNotPairs2() {
        MetricId.EMPTY.tagged("foo", "bar", "baz");
    }

    @Test
    public void testCompareTo() {
        final MetricId a = MetricId.EMPTY.tagged("foo", "bar");
        final MetricId b = MetricId.EMPTY.tagged("foo", "baz");

        assertTrue(a.compareTo(b) != 0);
        assertTrue(b.compareTo(a) != 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(b.resolve("key").compareTo(b) != 0);
        assertTrue(b.compareTo(b.resolve("key")) != 0);
    }

    @Test
    public void testCompareToMultipleTags() {
      final MetricId a = MetricId.EMPTY.tagged("a", "1", "b", "2");
      final MetricId b = MetricId.EMPTY.tagged("a", "1", "b", "3");
      final MetricId c = MetricId.EMPTY.tagged("a", "1", "b", "3");

      assertTrue(a.compareTo(a) == 0);
      assertTrue(a.compareTo(b) != 0);
      assertTrue(b.compareTo(b) == 0);
      assertTrue(b.compareTo(a) != 0);

      assertTrue(b.compareTo(c) == 0);
      assertTrue(c.compareTo(b) == 0);
    }

  @Test
    public void testEqualsAndHashCode() {
        // a map which always returns the same hashCode, but is equal to nothing.
        final SortedMap<String, String> tags = new TreeMap<String, String>() {
            public boolean equals(Object o) {
                return false;
            }

            ;

            public int hashCode() {
                return 42;
            }

            ;
        };

        final MetricId a = new MetricId(key, tags);
        final MetricId b = new MetricId(key, tags);

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, b);
    }
}
