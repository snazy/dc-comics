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
package com.dremio.cloud.comic.testing.impl;

import com.dremio.cloud.comic.api.ConfiguringContainer;
import com.dremio.cloud.comic.api.RuntimeContainer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ComicMonoLifecycle extends ComicParameterResolver implements BeforeAllCallback {

    private static volatile RuntimeContainer INSTANCE;

    @Override
    public void beforeAll(final ExtensionContext context) {
        if (INSTANCE == null) {
            synchronized (ComicMonoLifecycle.class) {
                if (INSTANCE == null) {
                    INSTANCE = ConfiguringContainer.of().start();
                    Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::close, getClass().getName() + "-shutdown"));
                }
            }
        }
        context.getStore(NAMESPACE).getOrComputeIfAbsent(RuntimeContainer.class, k -> INSTANCE);
    }

}
