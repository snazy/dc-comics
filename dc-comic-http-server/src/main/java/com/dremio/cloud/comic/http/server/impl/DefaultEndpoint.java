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

import com.dremio.cloud.comic.http.server.api.Request;
import com.dremio.cloud.comic.http.server.api.Response;
import com.dremio.cloud.comic.http.server.spi.Endpoint;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

public class DefaultEndpoint implements Endpoint {

    private final int priority;
    private final Predicate<Request> matcher;
    private final Function<Request, CompletionStage<Response>> handler;

    public DefaultEndpoint(final int priority, final Predicate<Request> matcher, final Function<Request, CompletionStage<Response>> handler) {
        this.priority = priority;
        this.matcher = matcher;
        this.handler = handler;
    }

    @Override
    public boolean matches(final Request request) {
        return matcher.test(request);
    }

    @Override
    public CompletionStage<Response> handle(final Request request) {
        return handler.apply(request);
    }

}
