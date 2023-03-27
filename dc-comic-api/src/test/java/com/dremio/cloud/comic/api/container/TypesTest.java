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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TypesTest {

    private final Types types = new Types();

    @Test
    void classes() {
        Assertions.assertTrue(types.isAssignable(Impl.class, Impl.class));
        Assertions.assertTrue(types.isAssignable(Api.class, Api.class));
        Assertions.assertTrue(types.isAssignable(Impl.class, Api.class));
        Assertions.assertTrue(types.isAssignable(Api.class, Impl.class));
    }

    @Test
    void parameterizedType() {
        Assertions.assertTrue(types.isAssignable(
                new Types.ParameterizedTypeImpl(List.class, Api.class),
                new Types.ParameterizedTypeImpl(List.class, Api.class)));
    }

    @Test
    void parameterizedTypeClass() {
        Assertions.assertTrue(types.isAssignable(
                GenericedImpl.class,
                new Types.ParameterizedTypeImpl(Generic.class, String.class)));
        Assertions.assertFalse(types.isAssignable(
                GenericedImpl.class,
                new Types.ParameterizedTypeImpl(Generic.class, Number.class)));
    }

    public interface Api {
    }

    public static class Impl implements Api {
    }

    public interface Generic<A> {
    }

    public static class GenericedImpl implements Generic<String> {
    }

}
