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
package com.dremio.cloud.comic.api.main;

import com.dremio.cloud.comic.api.ConfiguringContainer;
import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.container.bean.BaseBean;
import com.dremio.cloud.comic.api.scope.DefaultScoped;

import java.util.List;
import java.util.Map;

/**
 * Default launcher using {@link com.dremio.cloud.comic.api.ConfiguringContainer}/{@code com.dremio.cloud.comic.api.RuntimeContainer}.
 * It adds the notion of {@link Awaiter} to enable to not stop immediately when started.
 */
public final class Launcher {

    private Launcher() {
        // no-op
    }

    public static void main(final String... args) {
        // TODO forward args to Configuration props
        try (final var container = ConfiguringContainer.of()
                .register(new BaseBean<Args>(Args.class, DefaultScoped.class, 1000, Map.of()) {
                    @Override
                    public Args create(final RuntimeContainer container, final List<Instance<?>> dependents) {
                        return new Args(List.of(args));
                    }
                })
                .start();
             final var awaiters = container.lookups(Awaiter.class, list -> list.stream().map(Instance::instance).toList());) {
            awaiters.instance().forEach(Awaiter::await);
        }
    }

}
