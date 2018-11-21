package com.spotify.metrics.remote;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class LimitedRemoteTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    Remote inner;
    Remote outer;

    String url = "foo";
    Map<String, Object> json = ImmutableMap.of("a", (Object) "b");
    Map<String, Object> json2 = ImmutableMap.of("a", (Object) "c");
    Map<String, Object> json3 = ImmutableMap.of("a", (Object) "d");
    Map<String, Object> json4 = ImmutableMap.of("a", (Object) "e");

    @Before
    public void setUp() throws Exception {
        outer = new LimitedRemote(inner, 1, 2);
    }

    @Test
    public void createMeterTest() {
        outer.post(url, "foo", json);
        verify(inner).post(url, "foo", json);
    }

    @Test
    public void blockMeterTest() throws Exception {
        when(inner.post(anyString(), anyString(), anyMap())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return SettableFuture.<Integer>create();
            }
        });
        outer.post(url, "foo", json);
        outer.post(url, "foo", json2);
        verify(inner, times(1)).post(eq(url), eq("foo"), anyMap());
    }

    @Test
    public void overflowMeterTest() throws Exception {
        when(inner.post(anyString(), anyString(), anyMap())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return SettableFuture.<Integer>create();
            }
        });
        outer.post(url, "foo", json);
        outer.post(url, "foo", json2);
        outer.post(url, "foo", json3);
        ListenableFuture<Integer> overflow = outer.post(url, "foo", json4);

        assert (overflow.isDone());

        exception.expect(ExecutionException.class);
        overflow.get();
    }

}
