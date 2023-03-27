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
import com.dremio.cloud.comic.api.container.Generation;
import com.dremio.cloud.comic.api.lifecycle.Start;
import com.dremio.cloud.comic.api.lifecycle.Stop;
import com.dremio.cloud.comic.api.scope.DefaultScoped;
import com.dremio.cloud.comic.build.api.scanning.Injection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class ContainerTest {

    @Test
    void simple() {
        try (final var container = ConfiguringContainer.of()
                .register(new Bean1$ComicBean(), new Bean2$ComicBean())
                .start();
             final var lookup = container.lookup(Bean1.class)) {
            Assertions.assertEquals("bean1[bean2[]]", lookup.instance().toString());
        }
    }

    @Test
    void inheritance() {
        try (final var container = ConfiguringContainer.of()
                .register(new Bean1$ComicBean(), new Bean2$ComicBean())
                .start();
             final var lookup = container.lookup(Supplier.class)) {
            Assertions.assertEquals("bean1[bean2[]]", lookup.instance().toString());
        }
    }

    @Test
    void containerLifecycle() {
        final var startListener = new ArrayList<Start>();
        final var stopListener = new ArrayList<Stop>();
        try (final var ignored = ConfiguringContainer.of().register(
                new ComicListener<Start>() {
                    @Override
                    public Class<Start> eventType() {
                        return Start.class;
                    }

                    @Override
                    public void onEvent(final RuntimeContainer container, final Start event) {
                        startListener.add(event);
                    }
                },
                new ComicListener<Stop>() {
                    @Override
                    public Class<Stop> eventType() {
                        return Stop.class;
                    }

                    @Override
                    public void onEvent(final RuntimeContainer container, final Stop event) {
                        stopListener.add(event);
                    }
                })
                .start()) {
            Assertions.assertEquals(1, startListener.size());
            Assertions.assertTrue(stopListener.isEmpty());
        }
        Assertions.assertEquals(1, startListener.size());
        Assertions.assertEquals(1, stopListener.size());
    }

    @Generation(version = 1)
    public static class Bean1 implements Supplier<String> {
        @Injection
        private Bean2 bean2; // NOTE: private cause compilation as a single compilation unit

        @Override
        public String toString() {
            return "bean1[" + bean2 + "]";
        }

        @Override
        public String get() {
            return toString();
        }
    }

    @Generation(version = 1)
    public static class Bean1$ComicBean implements ComicBean<Bean1> {
        @Override
        public Type type() {
            return Bean1.class;
        }

        @Override
        public Class<?> scope() {
            return DefaultScoped.class;
        }

        @Override
        public Bean1 create(final RuntimeContainer container, final List<Instance<?>> dependents) {
            final var instance = new Bean1();
            {
                final var instance__bean2 = container.lookup(Bean2.class);
                instance.bean2 = instance__bean2.instance();
                dependents.add(instance__bean2);
            }
            return instance;
        }
    }

    public static class Bean2 {
        @Override
        public String toString() {
            return "bean2[]";
        }
    }

    public static class Bean2$ComicBean implements ComicBean<Bean2> {
        @Override
        public Type type() {
            return Bean2.class;
        }

        @Override
        public Class<?> scope() {
            return DefaultScoped.class;
        }

        @Override
        public Bean2 create(final RuntimeContainer container, final List<Instance<?>> dependents) {
            return new Bean2();
        }
    }

}
