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

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class Beans {

    private final Map<Type, List<ComicBean<?>>> beans = new HashMap<>();

    public Map<Type, List<ComicBean<?>>> getBeans() {
        return beans;
    }

    public void doRegister(final ComicBean<?>... beans) {
        this.beans.putAll(Stream.of(beans).collect(groupingBy(
                ComicBean::type,
                collectingAndThen(toList(), l -> l.stream()
                        .sorted(Comparator.<ComicBean<?>, Integer>comparing(ComicBean::priority).reversed())
                        .toList()))));
    }

}
