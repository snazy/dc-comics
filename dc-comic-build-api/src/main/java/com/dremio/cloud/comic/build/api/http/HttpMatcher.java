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
package com.dremio.cloud.comic.build.api.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method taking as input parameter a {@code com.dremio.cloud.comic.http.server.api.Request} and returning a
 * {@code java.util.concurrent.CompletionStage<com.dremio.cloud.comic.http.server.api.Response>}.
 * <p>
 * When matcher condition are met the endpoint will be called.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HttpMatcher {

    /**
     * Endpoint priority.
     * Enables to use a fallback endpoint by matching all methods and paths (or a subpart) and use it only if none of the
     * other endpoints match.
     *
     * @return endpoint priority
     */
    int priority() default 1000;

    /**
     * @return the HTTP method(s) to match, empty means all.
     */
    String[] methods() default {};

    PathMatching pathMatching() default PathMatching.IGNORED;

    /**
     * @return the path to match respecting {@link #pathMatching()} matching.
     */
    String path() default "";

    enum PathMatching {
        /**
         * Path is not matched - ignored.
         */
        IGNORED,

        /**
         * Path is exactly the configured value.
         */
        EXACT,

        /**
         * Path starts with the configured value.
         */
        STARTS_WITH,

        /**
         * Path ends with the configured value.
         */
        ENDS_WITH,

        /**
         * Path matches the configured regex.
         * IMPORTANT: take care your regex is not vulnerable to attacks (take care of the wildcards).
         * <p>
         * If used, a {@link java.util.regex.Pattern} will be set in request attribute {@code comic.http.matcher}.
         */
        REGEX
    }

}
