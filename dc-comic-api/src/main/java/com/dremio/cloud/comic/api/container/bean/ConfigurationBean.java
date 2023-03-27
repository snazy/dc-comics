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
package com.dremio.cloud.comic.api.container.bean;

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.configuration.ConfigurationSource;
import com.dremio.cloud.comic.api.container.configuration.ConfigurationImpl;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;

import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparing;

public class ConfigurationBean extends BaseBean<Configuration> {

    public ConfigurationBean() {
        super(Configuration.class, ApplicationScoped.class, 1000, Map.of());
    }

    @Override
    public Configuration create(final RuntimeContainer container, final List<Instance<?>> dependents) {
        return new ConfigurationImpl(lookups(
                container, ConfigurationSource.class,
                i -> i.stream()
                        .sorted(comparing(inst -> inst.bean().priority()))
                        .map(Instance::instance)
                        .toList(),
                dependents));
    }

}
