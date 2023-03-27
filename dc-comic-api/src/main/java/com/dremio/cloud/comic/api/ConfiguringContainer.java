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
package com.dremio.cloud.comic.api;

import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.container.ComicListener;
import com.dremio.cloud.comic.api.container.ComicModule;
import com.dremio.cloud.comic.api.container.ConfiguringContainerImpl;
import com.dremio.cloud.comic.api.spi.ComicContext;

public interface ConfiguringContainer {

    static ConfiguringContainer of() {
        return new ConfiguringContainerImpl();
    }

    RuntimeContainer start();

    ConfiguringContainer disableAutoDiscovery(boolean disableAutoDiscovery);

    ConfiguringContainer loader(ClassLoader loader);

    ConfiguringContainer register(ComicModule... modules);

    ConfiguringContainer register(ComicBean<?>... beans);

    ConfiguringContainer register(ComicListener<?>... listeners);

    ConfiguringContainer register(ComicContext... contexts);

}
