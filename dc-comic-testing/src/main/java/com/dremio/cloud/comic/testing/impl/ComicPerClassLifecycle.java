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
import com.dremio.cloud.comic.api.container.ComicModule;
import com.dremio.cloud.comic.testing.ComicSupport;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class ComicPerClassLifecycle extends ComicParameterResolver implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(final ExtensionContext context) {
        context.getStore(NAMESPACE).getOrComputeIfAbsent(RuntimeContainer.class, k -> {
            final var container = ConfiguringContainer.of();
            AnnotationUtils.findAnnotation(context.getTestClass(), ComicSupport.class)
                    .ifPresent(conf -> {
                        container.disableAutoDiscovery(conf.disableDiscovery());
                        container.register(Stream.of(conf.modules())
                                .map(it -> {
                                    try {
                                        return it.asSubclass(ComicModule.class).getConstructor().newInstance();
                                    } catch (final InstantiationException | IllegalAccessException |
                                                   NoSuchMethodException e) {
                                        throw new IllegalStateException(e);
                                    } catch (final InvocationTargetException e) {
                                        throw new IllegalStateException(e.getTargetException());
                                    }
                                })
                                .toArray(ComicModule[]::new));
                    });
            return container.start();
        });
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        super.afterAll(context);
        ofNullable(context.getStore(NAMESPACE).get(RuntimeContainer.class, RuntimeContainer.class))
                .ifPresent(RuntimeContainer::close);
    }

}
