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
package com.dremio.cloud.comic.api.event;

/**
 * Injectable type to emit container events (see {@link com.dremio.cloud.comic.build.api.event.OnEvent} and {@link com.dremio.cloud.comic.api.RuntimeContainer#emit(Object)}
 */
public interface Emitter {

    <T> void emit(T event);

}
