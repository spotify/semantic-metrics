/*
 * Copyright (c) 2016 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.metrics.remote;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Simple Remote implementation using OkHTTP
 */
public class OkRemote implements Remote {
    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON
        = MediaType.parse("application/json; charset=utf-8");
    private final String host;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();

    public OkRemote(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public ListenableFuture<Integer> post(String path, String shardKey, Map jsonObj) {
        if ((path.length() > 0) && (path.charAt(0) != '/')) {
            path = "/" + path;
        }
        final String jsonStr;
        try {
            jsonStr = mapper.writeValueAsString(jsonObj);
        } catch (JsonProcessingException e) {
            return Futures.immediateFailedFuture(new RuntimeException("Invalid json input"));
        }
        final String url = "http://" + host + ":" + port + path;
        final RequestBody body = RequestBody.create(JSON, jsonStr);

        final Request request = new Request.Builder()
            .url(url)
            .addHeader(Sharder.SHARD_KEY, shardKey)
            .addHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE)
            .post(body)
            .build();
        final SettableFuture<Integer> result = SettableFuture.create();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.set(503);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                result.set(response.code());
            }
        });
        return result;
    }

    @Override
    public void waitForAllCalls() {
        Dispatcher dispatcher = client.dispatcher();
        while (dispatcher.queuedCallsCount() != 0 || dispatcher.runningCallsCount() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
