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
package com.dremio.cloud.comic.json.internal.framework;

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.json.JsonMapper;
import com.dremio.cloud.comic.json.internal.JsonMapperImpl;
import com.dremio.cloud.comic.json.serialization.JsonCodec;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonMapperBean implements ComicBean<JsonMapper> {

    @Override
    public Type type() {
        return JsonMapper.class;
    }

    @Override
    public Class<?> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public JsonMapper create(final RuntimeContainer container, final List<Instance<?>> dependents) {
        final var codecs = container.lookups(JsonCodec.class, i -> i.stream().map(it -> (JsonCodec<?>) it.instance()).toList());
        dependents.add(codecs);
        final var conf = container.lookup(Configuration.class);
        dependents.add(conf);
        return new JsonMapperImpl(new ArrayList<>(codecs.instance()), conf.instance());
    }

    @Override
    public void destroy(final RuntimeContainer container, final JsonMapper instance) {
        instance.close();
    }

}
