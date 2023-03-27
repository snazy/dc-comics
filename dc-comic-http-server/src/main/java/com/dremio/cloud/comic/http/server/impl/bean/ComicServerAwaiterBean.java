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
import com.dremio.cloud.comic.api.container.bean.BaseBean;
import com.dremio.cloud.comic.api.main.Awaiter;
import com.dremio.cloud.comic.api.scope.DefaultScoped;
import com.dremio.cloud.comic.http.server.api.WebServer;

import java.util.List;
import java.util.Map;

public class ComicServerAwaiterBean extends BaseBean<Awaiter> {

    public ComicServerAwaiterBean() {
        super(Awaiter.class, DefaultScoped.class, 1000, Map.of());
    }

    @Override
    public Awaiter create(final RuntimeContainer container, final List<Instance<?>> dependents) {
        return () -> {
            try (final var server = container.lookup(WebServer.class)) {
                server.instance().await();
            }
        };
    }

}
