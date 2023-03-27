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
import com.dremio.cloud.comic.api.container.DefaultInstance;
import com.dremio.cloud.comic.api.scope.DefaultScoped;
import com.dremio.cloud.comic.api.spi.ComicContext;

import java.util.ArrayList;
import java.util.Collections;

public class DefaultComicContext implements ComicContext {

    @Override
    public Class<?> marker() {
        return DefaultScoped.class;
    }

    @Override
    public <T> Instance<T> getOrCreate(final RuntimeContainer container, final ComicBean<T> bean) {
        final var dependents = new ArrayList<Instance<?>>();
        final var instance = bean.create(container, dependents);
        Collections.reverse(dependents); // destroy in reverse order
        return new DefaultInstance<>(bean, container, instance, dependents);
    }

}
