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
package com.dremio.cloud.comic.api.container.configuration.source;

import com.dremio.cloud.comic.api.configuration.ConfigurationSource;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class EnvironmentSource implements ConfigurationSource {

    private final Pattern posix = Pattern.compile("[^A-Za-z0-9]");

    @Override
    public String get(final String key) {
        return Optional.ofNullable(System.getenv(key))
                .or(() -> {
                    final var posixKey = posix.matcher(key).replaceAll("_");
                    return Optional.ofNullable(System.getenv(posixKey))
                            .or(() -> Optional.ofNullable(System.getenv(posixKey.toUpperCase(Locale.ROOT))));
                })
                .orElse(null);
    }

}
