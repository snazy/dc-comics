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
package com.dremio.cloud.comic.json.internal.codec;

import com.dremio.cloud.comic.api.container.Types;
import com.dremio.cloud.comic.json.internal.JsonStrings;
import com.dremio.cloud.comic.json.internal.parser.JsonParser;
import com.dremio.cloud.comic.json.serialization.JsonCodec;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dremio.cloud.comic.json.spi.Parser.Event.*;

public class MapJsonCodec<A> implements JsonCodec<Map<String, A>> {

    private final JsonCodec<A> delegate;
    private final Type type;

    public MapJsonCodec(final JsonCodec<A> delegate) {
        this.delegate = delegate;
        this.type = new Types.ParameterizedTypeImpl(Map.class, String.class, delegate.type());
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Map<String, A> read(final DeserializationContext context) throws IOException {
        final var reader = context.parser();
        reader.enforceNext(START_OBJECT);

        final var instance = new LinkedHashMap<String, A>();
        JsonParser.Event event;
        while (reader.hasNext() && (event = reader.next()) != END_OBJECT) {
            reader.rewind(event);

            final var keyEvent = reader.next();
            if (keyEvent != KEY_NAME) {
                throw new IllegalStateException("Expected=KEY_NAME, but got " + keyEvent);
            }
            instance.put(reader.getString(), delegate.read(context));
        }
        return instance;
    }

    @Override
    public void write(final Map<String, A> value, final SerializationContext context) throws IOException {
        final var writer = context.writer();
        final var it = value.entrySet().iterator();
        writer.write('{');
        while (it.hasNext()) {
            final var entry = it.next();
            if (entry == null) {
                continue;
            }

            writer.write(JsonStrings.escape(entry.getKey()) + ":");
            if (entry.getValue() == null) {
                writer.write("null");
            } else {
                delegate.write(entry.getValue(), context);
            }
            if (it.hasNext()) {
                writer.write(',');
            }
        }
        writer.write('}');
    }

}
