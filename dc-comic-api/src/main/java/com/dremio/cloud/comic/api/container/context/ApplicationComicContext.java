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
package com.dremio.cloud.comic.api.container.context;

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.container.context.subclass.DelegatingContext;
import com.dremio.cloud.comic.api.container.context.subclass.SupplierDelegatingContext;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.api.spi.ComicContext;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class ApplicationComicContext extends DefaultComicContext implements ComicContext, AutoCloseable {

    private final Map<ComicBean<?>, ApplicationInstance<?>> instances = new ConcurrentHashMap<>();

    @Override
    public Class<?> marker() {
        return ApplicationScoped.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Instance<T> getOrCreate(final RuntimeContainer container, final ComicBean<T> bean) {
        final var fastExisting = instances.get(bean); // do NOT use computeIfAbsent since it can be recursive and therefore fail
        if (fastExisting != null) {
            return (Instance<T>) fastExisting;
        }

        synchronized (bean) { // beans are singleton for a container so we can lock on them securely, in particular there in app ctx
            final var existing = instances.get(bean);
            if (existing != null) {
                return (Instance<T>) existing;
            }

            // don't create the instance directly, ensure it is created lazily at need
            final var subclass = (Function<DelegatingContext<T>, T>) bean.data().get("comic.subclasses.delegate");
            final var created = new ApplicationInstance<>(subclass, () -> super.getOrCreate(container, bean), bean);
            instances.put(bean, created);
            return created;
        }
    }

    @Override
    public void close() {
        final var error = new IllegalStateException("Can't release all singletons, see suppressed exceptions for details");
        instances.values().stream().map(i -> i.real).filter(Objects::nonNull).forEach(i -> {
            try {
                i.close();
            } catch (final RuntimeException re) {
                error.addSuppressed(re);
            }
        });
        instances.clear();
        if (error.getSuppressed().length > 0) {
            throw error;
        }
    }

    private static class ApplicationInstance<T> implements Instance<T> {

        private final Supplier<Instance<T>> factory;
        private final ComicBean<T> bean;
        private final T proxy;
        private volatile Instance<T> real;

        private ApplicationInstance(final Function<DelegatingContext<T>, T> subclassFactory, final Supplier<Instance<T>> real, final ComicBean<T> bean) {
            this.factory = real;
            this.bean = bean;
            if (subclassFactory != null) { // the instance is created at first call (most lazy possible)
                this.proxy = subclassFactory.apply(new SupplierDelegatingContext<>(() -> ensureDelegate().instance()));
            } else { // instance is created at first read (generally injection time so way earlier)
                this.proxy = null;
            }
        }

        private Instance<T> ensureDelegate() {
            if (real != null) {
                return real;
            }
            synchronized (factory) {
                if (real == null) {
                    real = factory.get();
                }
            }
            return real;
        }

        @Override
        public ComicBean<T> bean() {
            return bean;
        }

        @Override
        public T instance() {
            return proxy == null ? ensureDelegate().instance() : proxy;
        }

        @Override
        public void close() {
            // no-op, will be done with context destruction
        }

    }

}
