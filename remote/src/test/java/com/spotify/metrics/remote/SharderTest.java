package com.spotify.metrics.remote;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SharderTest {
    @Test
    public void shardKeyTest() {
        String key = Sharder.buildShardKey(ImmutableList.of("what", "status"),
                ImmutableMap.of("what","balls","status","tripping","site","lon"));
        assertEquals("what:balls,status:tripping", key);
    }
}
