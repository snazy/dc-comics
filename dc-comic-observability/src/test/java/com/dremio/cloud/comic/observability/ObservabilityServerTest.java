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
package com.dremio.cloud.comic.observability;

import com.dremio.cloud.comic.http.server.api.WebServer;
import com.dremio.cloud.comic.observability.health.HealthCheck;
import com.dremio.cloud.comic.observability.health.HealthRegistry;
import com.dremio.cloud.comic.observability.http.ObservabilityServer;
import com.dremio.cloud.comic.observability.metrics.MetricsRegistry;
import com.dremio.cloud.comic.observability.test.SampleCheck;
import com.dremio.cloud.comic.testing.Comic;
import com.dremio.cloud.comic.testing.ComicSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ComicSupport
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObservabilityServerTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void metrics(@Comic final ObservabilityServer server, @Comic final WebServer webServer,
                 @Comic final MetricsRegistry registry) throws IOException, InterruptedException {
        registry.registerReadOnlyGauge("my_gauge", "value", () -> 100);
        {
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + server.getPort() + "/metrics"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("""
                            # TYPE my_gauge gauge
                            # UNIT my_gauge value
                            my_gauge 100
                            # EOF""",
                    response.body());
        }
        {
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + webServer.configuration().port() + "/metrics"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode());
        }
        registry.unregisterGauge("my_gauge");
    }

    @Test
    void health(@Comic final ObservabilityServer server, @Comic final WebServer webServer,
                @Comic final HealthRegistry registry,
                @Comic final SampleCheck check) throws IOException, InterruptedException {
        {
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + server.getPort() + "/health"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("test-check,OK,\"worked\"", response.body());
        }

        final var oldCheck = check.check();

        final var failure = new CompletableFuture<HealthCheck.Result>();
        failure.completeExceptionally(new IllegalStateException("oops"));
        check.setCheck(failure);
        try {
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + server.getPort() + "/health"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode());
            assertEquals("test-check,KO,\"java.lang.IllegalStateException: oops\"", response.body());
        } finally {
            check.setCheck(oldCheck);
        }

        final var ko = new CompletableFuture<HealthCheck.Result>();
        ko.complete(new HealthCheck.Result(HealthCheck.Status.KO, "oops from test"));
        check.setCheck(ko);
        try {
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + server.getPort() + "/health"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode());
            assertEquals("test-check,KO,\"oops from test\"", response.body());
        } finally {
            check.setCheck(oldCheck);
        }

        {
            final var response = client.send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(URI.create("http://localhost:" + webServer.configuration().port() + "/health"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode());
        }
    }

}
