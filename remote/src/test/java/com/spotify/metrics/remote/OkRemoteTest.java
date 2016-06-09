package com.spotify.metrics.remote;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class OkRemoteTest {

    Remote remote;
    MockWebServer server;

    @Before
    public void setUp() {
        server = new MockWebServer();
        remote = new OkRemote(server.url("/").host(), server.url("/").port());
    }

    @Test
    public void httpTest() throws InterruptedException, ExecutionException {
        server.enqueue(new MockResponse().setBody("hia"));
        ListenableFuture<Integer> result = remote.post("foo", "foo", ImmutableMap.of("aaa", "bbb"));
        RecordedRequest r = server.takeRequest();
        assertEquals("{\"aaa\":\"bbb\"}", r.getBody().readUtf8());
        assertEquals(1, server.getRequestCount());
        assertEquals("/foo", r.getPath());
        assertEquals((Integer) 200, result.get());
    }

    @Test
    public void shardTest() throws InterruptedException, ExecutionException {
        server.enqueue(new MockResponse().setBody("hia"));
        ListenableFuture<Integer> result = remote.post("foo", "shard-key", ImmutableMap.of());
        RecordedRequest r = server.takeRequest();
        assertEquals("shard-key", r.getHeader("X-Shard-Key"));
    }
}
