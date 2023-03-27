/*
 * Copyright (c) 2023 - Dremio - https://www.dremio.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.dremio.cloud.comic.http.server.impl;

import com.dremio.cloud.comic.http.server.api.Cookie;
import com.dremio.cloud.comic.http.server.api.IOConsumer;
import com.dremio.cloud.comic.http.server.api.Response;
import com.dremio.cloud.comic.http.server.impl.flow.BytesPublisher;
import com.dremio.cloud.comic.http.server.impl.flow.WriterPublisher;

import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

public class ComicResponse implements Response {

    private final int status;
    private final Map<String, List<String>> headers;
    private final List<Cookie> cookies;
    private final Flow.Publisher<ByteBuffer> body;

    private ComicResponse(final int status, final Map<String, List<String>> headers, final List<Cookie> cookies,
                           final Flow.Publisher<ByteBuffer> body) {
        this.status = status;
        this.headers = headers;
        this.cookies = cookies;
        this.body = body;
    }

    @Override
    public int status() {
        return status;
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers == null ? Map.of() : headers;
    }

    @Override
    public List<Cookie> cookies() {
        return cookies == null ? List.of() : cookies;
    }

    @Override
    public Flow.Publisher<ByteBuffer> body() {
        return body;
    }

    public static class Builder implements Response.Builder {
        private int status = 200;
        private Map<String, List<String>> headers;
        private List<Cookie> cookies;
        private Flow.Publisher<ByteBuffer> body;

        @Override
        public Response.Builder status(final int value) {
            this.status = value;
            return this;
        }

        @Override
        public Response.Builder header(final String key, final String value) {
            if (headers == null) {
                headers = new LinkedHashMap<>();
            }
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            return this;
        }

        @Override
        public Response.Builder cookie(final Cookie cookie) {
            if (cookies == null) {
                cookies = new ArrayList<>();
            }
            cookies.add(cookie);
            return this;
        }

        @Override
        public Response.Builder body(final Flow.Publisher<ByteBuffer> writer) {
            this.body = writer;
            return this;
        }

        @Override
        public Response.Builder body(final String body) {
            return body(new BytesPublisher(body));
        }

        @Override
        public Response.Builder body(final IOConsumer<Writer> bodyHandler) {
            return body(new WriterPublisher(bodyHandler));
        }

        @Override
        public Response build() {
            return new ComicResponse(status, headers, cookies, body);
        }
    }

}
