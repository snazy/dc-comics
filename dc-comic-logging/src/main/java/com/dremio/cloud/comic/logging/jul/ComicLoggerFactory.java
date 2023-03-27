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
package com.dremio.cloud.comic.logging.jul;

public final class ComicLoggerFactory {

    private static volatile ComicLoggers delegate;

    private ComicLoggerFactory() {
        // no-op
    }

    public static ComicLoggers get() {
        if (delegate == null) {
            synchronized (ComicLoggers.class) {
                if (delegate == null) {
                    delegate = new ComicLoggers();
                }
            }
        }
        return delegate;
    }

    // must be called only in a safe way
    public static void set(final ComicLoggers loggers) {
        if (delegate != null) {
            delegate.close();
        }
        delegate = loggers;
    }

}
