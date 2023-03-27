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

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;

import java.util.List;

public class DefaultInstance<T> implements Instance<T> {

    private final ComicBean<T> bean;
    private final RuntimeContainer container;
    private final T instance;
    private final List<Instance<?>> dependencies;

    public DefaultInstance(final ComicBean<T> bean, final RuntimeContainer container, final T instance, final List<Instance<?>> dependencies) {
        this.bean = bean;
        this.container = container;
        this.instance = instance;
        this.dependencies = dependencies;
    }

    @Override
    public ComicBean<T> bean() {
        return bean;
    }

    @Override
    public T instance() {
        return instance;
    }

    @Override
    public synchronized void close() {
        if (bean != null) {
            bean.destroy(container, instance);
        }

        if (dependencies.isEmpty()) {
            return;
        }

        RuntimeException error = null;
        for (final var dep : dependencies) {
            try {
                dep.close();
            } catch (final Exception re) {
                if (error == null) {
                    error = new IllegalStateException("Can't close properly dependencies of " + instance);
                }
                error.addSuppressed(re);
            }
        }
        dependencies.clear();
        if (error != null) {
            throw error;
        }
    }

}
