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

import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.container.ComicListener;
import com.dremio.cloud.comic.api.lifecycle.Start;
import com.dremio.cloud.comic.http.server.api.WebServer;

public class ComicServerStarterListener implements ComicListener<Start> {

    @Override
    public int priority() {
        return 2000;
    }

    @Override
    public Class<Start> eventType() {
        return Start.class;
    }

    @Override
    public void onEvent(final RuntimeContainer container, final Start event) {
        // close() in case but it is app scoped normally so no-op
        try (final var configuration = container.lookup(Configuration.class)) {
            if (!configuration.instance().get("comic.http-server.start").map(Boolean::parseBoolean).orElse(true)) {
                return;
            }

            try (final var server = container.lookup(WebServer.class)) {
                server.instance(); // force a lookup to start it
            }
        }
    }

}
