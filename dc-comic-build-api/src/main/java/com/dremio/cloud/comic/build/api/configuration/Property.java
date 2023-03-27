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
package com.dremio.cloud.comic.build.api.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface Property {

    String NO_VALUE = "com.dremio.cloud.comic.build.api.configuration.Property.NO_VALUE";

    /**
     * @return name of the property - else the field/member name is used.
     */
    String value() default "";

    /**
     * IMPORTANT: the default value is directly injected as value if none is configured, ensure it is valid java.
     *
     * @return default value to use.
     */
    String defaultValue() default NO_VALUE;

    /**
     * @return {@code true} if it should fail at runtime if the value is missing.
     */
    boolean required() default false;

    /**
     * @return some comment about the property goal/intent/usage.
     */
    String documentation() default "";

}
