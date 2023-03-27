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

import com.dremio.cloud.comic.api.ConfiguringContainer;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.container.bean.ConfigurationBean;
import com.dremio.cloud.comic.api.container.bean.ProvidedInstanceBean;
import com.dremio.cloud.comic.api.container.context.ApplicationComicContext;
import com.dremio.cloud.comic.api.container.context.DefaultComicContext;
import com.dremio.cloud.comic.api.event.Emitter;
import com.dremio.cloud.comic.api.lifecycle.Start;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.api.scope.DefaultScoped;
import com.dremio.cloud.comic.api.spi.ComicContext;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class ConfiguringContainerImpl implements ConfiguringContainer {

    private final Beans beans = new Beans();
    private final Contexts contexts = new Contexts();
    private final Listeners listeners = new Listeners();
    private final Collection<ComicModule> modules = new ArrayList<>();
    private boolean disableAutoDiscovery = false;
    private ClassLoader loader = Thread.currentThread().getContextClassLoader();

    @Override
    public RuntimeContainer start() {
        final var runtimeContainer = new RuntimeContainerImpl(beans, contexts, listeners);
        if (disableAutoDiscovery && modules.isEmpty()) {
            contexts.doRegister(new ApplicationComicContext(), new DefaultComicContext());
            beans.doRegister(defaultBeans(runtimeContainer).toArray(ComicBean<?>[]::new));
            runtimeContainer.clearCache();
            if (listeners.hasDirectListener(Start.class)) {
                listeners.fire(runtimeContainer, new Start());
            }
            return runtimeContainer;
        }

        // modules
        final var modules = disableAutoDiscovery ?
                this.modules :
                loadModules()
                        .sorted(comparing(ComicModule::priority))
                        .toList();

        // beans
        beans.doRegister(filter(
                Stream.concat(
                        modules.stream().flatMap(ComicModule::beans),
                        defaultBeans(runtimeContainer)),
                modules.stream().map(ComicModule::beanFilter),
                runtimeContainer)
                .toArray(ComicBean<?>[]::new));

        // contexts
        contexts.doRegister(filter(
                Stream.concat(
                        // default scopes
                        Stream.of(new ApplicationComicContext(), new DefaultComicContext()),
                        // discovered ones (through module)
                        modules.stream().flatMap(ComicModule::contexts)),
                modules.stream().map(ComicModule::contextFilter),
                runtimeContainer)
                        .toArray(ComicContext[]::new));

        // listeners
        listeners.doRegister(filter(
                modules.stream().flatMap(ComicModule::listeners),
                modules.stream().map(ComicModule::listenerFilter),
                runtimeContainer)
                .toArray(ComicListener[]::new));

        // startup event
        runtimeContainer.clearCache();
        if (listeners.hasDirectListener(Start.class)) {
            listeners.fire(runtimeContainer, new Start());
        }

        return runtimeContainer;
    }

    @Override
    public com.dremio.cloud.comic.api.ConfiguringContainer disableAutoDiscovery(final boolean disableAutoDiscovery) {
        this.disableAutoDiscovery = disableAutoDiscovery;
        return this;
    }

    @Override
    public com.dremio.cloud.comic.api.ConfiguringContainer loader(final ClassLoader loader) {
        this.loader = loader;
        return this;
    }

    @Override
    public com.dremio.cloud.comic.api.ConfiguringContainer register(final ComicModule... modules) {
        this.modules.addAll(List.of(modules));
        return this;
    }

    @Override
    public com.dremio.cloud.comic.api.ConfiguringContainer register(final ComicBean<?>... beans) {
        this.beans.doRegister(beans);
        return this;
    }

    @Override
    public com.dremio.cloud.comic.api.ConfiguringContainer register(final ComicListener<?>... listeners) {
        this.listeners.doRegister(listeners);
        return this;
    }

    @Override
    public com.dremio.cloud.comic.api.ConfiguringContainer register(final ComicContext... contexts) {
        this.contexts.doRegister(contexts);
        return this;
    }

    protected Stream<ComicModule> loadModules() {
        return ServiceLoader
                .load(ComicModule.class, loader).stream()
                .map(ServiceLoader.Provider::get);
    }

    protected Stream<ComicBean<?>> defaultBeans(final RuntimeContainer runtimeContainer) {
        return Stream.of(
                new ProvidedInstanceBean<>(ApplicationScoped.class, Emitter.class, () -> runtimeContainer),
                new ProvidedInstanceBean<>(DefaultScoped.class, RuntimeContainer.class, () -> runtimeContainer),
                new ConfigurationBean());
    }

    private <A> Stream<A> filter(final Stream<A> input, final Stream<BiPredicate<RuntimeContainer, A>> predicates, final RuntimeContainer runtimeContainer) {
        final var predicate = predicates.filter(Objects::nonNull).reduce(null, (a, b) -> a == null ? b : a.and(b));
        return predicate == null ? input : input.filter(it -> predicate.test(runtimeContainer, it));
    }

}
