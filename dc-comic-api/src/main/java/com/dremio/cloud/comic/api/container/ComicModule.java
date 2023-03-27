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
package com.dremio.cloud.comic.api.container;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.spi.ComicContext;

public interface ComicModule {

    /**
     * @return module priority.
     */
    default int priority() {
        return 1000;
    }

    /**
     * @return the stream of beans to register.
     */
    default Stream<ComicBean<?>> beans() {
        return Stream.empty();
    }

    /**
     * @return the stream of contexts to register.
     */
    default Stream<ComicContext> contexts() {
        return Stream.empty();
    }

    /**
     * @return the stream of event listeners to register.
     */
    default Stream<ComicListener<?>> listeners() {
        return Stream.empty();
    }

    default Map<String, Object> data() {
        return Map.of();
    }

    /**
     * IMPORTANT: you cannot use lookup functions yet on the container until you know it will work (priority can help).
     * <p>
     * Enables to replace easily a bean.
     *
     * @return null if no filter should be applied (faster than an always true predicate) or a predicate filtering beans.
     */
    default BiPredicate<RuntimeContainer, ComicBean<?>> beanFilter() {
        return null;
    }

    /**
     * IMPORTANT: you cannot use lookup functions yet on the container until you know it will work (priority can help).
     * <p>
     * Enables to replace a context if needed.
     *
     * @return null if no filter should be applied (faster than an always true predicate) or a predicate filtering beans.
     */
    default BiPredicate<RuntimeContainer, ComicContext> contextFilter() {
        return null;
    }

    /**
     * IMPORTANT: you cannot use lookup functions yet on the container until you know it will work (priority can help).
     * <p>
     * Enables to replace a listener or disable it easily.
     *
     * @return null if no filter should be applied (faster than an always true predicate) or a predicate filtering beans.
     */
    default BiPredicate<RuntimeContainer, ComicListener<?>> listenerFilter() {
        return null;
    }

}
