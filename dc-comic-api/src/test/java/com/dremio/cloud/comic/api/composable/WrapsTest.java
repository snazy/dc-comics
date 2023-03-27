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
package com.dremio.cloud.comic.api.composable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.dremio.cloud.comic.api.composable.Wraps.wrap;

public class WrapsTest {

    @Test
    void wrapSupplier() {
        Assertions.assertEquals("ioc",
                wrap(
                        () -> "c",
                        delegate -> () -> "i" + delegate.get(),
                        delegate -> () -> "o" + delegate.get()));
    }

    @Test
    void wrapRunnable() {
        final var list = new ArrayList<String>();
        wrap(
                () -> {
                    list.add("last");
                },
                delegate -> () -> {
                    list.add("first");
                    return delegate.get();
                },
                delegate -> () -> {
                    list.add("second");
                    return delegate.get();
                });
        Assertions.assertEquals(List.of("first", "second", "last"), list);
    }

}
