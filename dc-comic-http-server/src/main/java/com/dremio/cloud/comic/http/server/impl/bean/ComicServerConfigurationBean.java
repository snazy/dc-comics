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
package com.dremio.cloud.comic.http.server.impl.bean;

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.container.bean.BaseBean;
import com.dremio.cloud.comic.api.event.Emitter;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.http.server.api.WebServer;
import com.dremio.cloud.comic.http.server.impl.tomcat.TomcatWebServerConfiguration;
import com.dremio.cloud.comic.http.server.spi.Endpoint;

import java.util.List;
import java.util.Map;

// configuration as a beam to ensure it can be injected - at least to get the port
public class ComicServerConfigurationBean extends BaseBean<WebServer.Configuration> {

    public ComicServerConfigurationBean() {
        super(WebServer.Configuration.class, ApplicationScoped.class, 1000, Map.of());
    }

    @Override
    public WebServer.Configuration create(final RuntimeContainer container, final List<Instance<?>> dependents) {
        final var configuration = WebServer.Configuration.of();
        try (final var conf = container.lookup(Configuration.class)) {
            final var confAccessor = conf.instance();
            confAccessor.get("comic.http-server.port").map(Integer::parseInt).ifPresent(configuration::port);
            confAccessor.get("comic.http-server.host").ifPresent(configuration::host);
            confAccessor.get("comic.http-server.accessLogPattern").ifPresent(configuration::accessLogPattern);
            confAccessor.get("comic.http-server.base").ifPresent(configuration::base);
            confAccessor.get("comic.http-server.comicServletMapping").ifPresent(configuration::comicServletMapping);
            confAccessor.get("comic.http-server.utf8Setup").map(Boolean::parseBoolean).ifPresent(configuration::utf8Setup);
        }

        configuration
                .unwrap(TomcatWebServerConfiguration.class)
                .setEndpoints(lookups(
                        container, Endpoint.class,
                        l -> l.stream().map(Instance::instance).toList(),
                        dependents));

        try (final var instance = container.lookup(Emitter.class)) { // enable a listener to customize the configuration
            instance.instance().emit(configuration);
        }

        return configuration;
    }

}
