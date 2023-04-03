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
package com.dremio.cloud.comic.observability.metrics;

import com.dremio.cloud.comic.http.server.api.Request;
import com.dremio.cloud.comic.http.server.api.Response;
import com.dremio.cloud.comic.http.server.spi.Endpoint;

import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class Metrics implements Endpoint {

    private final MetricsRegistry registry;
    private final OpenMetricsFormatter formatter = new OpenMetricsFormatter();

    public Metrics(final MetricsRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean matches(final Request request) {
        return "GET".equalsIgnoreCase(request.method()) && "/metrics".equalsIgnoreCase(request.path());
    }

    @Override
    public CompletionStage<Response> handle(final Request request) {
        request.setAttribute("skip-access-log", true);
        return completedFuture(Response.of()
                .status(200)
                .header("content-type", "text/plain")
                .body(formatter.apply(registry.entries()))
                .build());
    }

}
