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
package com.dremio.cloud.comic.json.internal;

import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.container.Types;
import com.dremio.cloud.comic.json.JsonMapper;
import com.dremio.cloud.comic.json.internal.codec.*;
import com.dremio.cloud.comic.json.internal.parser.BufferProvider;
import com.dremio.cloud.comic.json.internal.parser.JsonParser;
import com.dremio.cloud.comic.json.serialization.JsonCodec;
import com.dremio.cloud.comic.json.spi.Parser;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class JsonMapperImpl implements JsonMapper {

    private final Map<Type, JsonCodec<?>> codecs;
    private final Function<Reader, Parser> parserFactory;

    public JsonMapperImpl(final Collection<JsonCodec<?>> jsonCodecs, final Configuration configuration) {
        this(jsonCodecs, configuration, createReaderParserFunction(configuration));
    }

    public JsonMapperImpl(final Collection<JsonCodec<?>> jsonCodecs, final Configuration configuration,
                          final Function<Reader, Parser> readerParserFunction) {
        this.parserFactory = readerParserFunction;

        this.codecs = new ConcurrentHashMap<>();
        this.codecs.putAll(toCodecMap(jsonCodecs.stream()));
        this.codecs.putAll(toCodecMap(builtInCodecs().filter(it -> !this.codecs.containsKey(it.type()))));
    }

    protected Stream<JsonCodec<?>> builtInCodecs() {
        return Stream.of( // do not forget to update com.dremio.cloud.comic.processor.internal.generator.JsonCodecGenerator if changing this
                new StringJsonCodec(),
                new IntegerJsonCodec(),
                new LongJsonCodec(),
                new DoubleJsonCodec(),
                new BigDecimalJsonCodec(),
                new BooleanJsonCodec(),
                new LocalDateJsonCodec(),
                new LocalDateTimeJsonCodec(),
                new ZonedDateTimeJsonCodec(),
                new OffsetDateTimeJsonCodec(),
                new ObjectJsonCodec());
    }

    @Override
    public <A> byte[] toBytes(final A instance) {
        final var out = new ByteArrayOutputStream();
        try (final var writer = new OutputStreamWriter(out, UTF_8)) {
            write(instance, writer);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    @Override
    public <A> A fromBytes(final Class<A> type, final byte[] bytes) {
        return fromBytes((Type) type, bytes);
    }

    @Override
    public <A> A fromBytes(final Type type, final byte[] bytes) {
        try (final var reader = new InputStreamReader(new ByteArrayInputStream(bytes), UTF_8)) {
            return read(type, reader);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <A> A fromString(final Class<A> type, final String string) {
        return fromString((Type) type, string);
    }

    @Override
    public <A> A fromString(final Type type, final String string) {
        try (final var reader = new StringReader(string)) {
            return read(type, reader);
        }
    }

    @Override
    public <A> String toString(final A instance) {
        final var writer = new StringWriter();
        try (writer) {
            write(instance, writer);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <A> void write(final A instance, final Writer writer) {
        try {
            if (instance == null) {
                writer.write("null");
                return;
            }

            if (instance instanceof Collection<?> collection) {
                if (collection.isEmpty()) {
                    writer.write("[]");
                    return;
                }

                final var firstItem = collection.stream().filter(Objects::nonNull).findFirst().orElse(null);
                if (firstItem != null) {
                    if (firstItem instanceof Map<?, ?>) { // consider it is just an object
                        final JsonCodec jsonCodec = codecs.get(Object.class);
                        jsonCodec.write(collection, new JsonCodec.SerializationContext(writer, this::codecLookup));
                        return;
                    }

                    final var itemClass = firstItem.getClass();

                    final var key = new Types.ParameterizedTypeImpl(Collection.class, itemClass);
                    final JsonCodec existing = codecs.get(key);
                    if (existing != null) {
                        existing.write(collection, new JsonCodec.SerializationContext(writer, this::codecLookup));
                        return;
                    }

                    final var itemCodec = (JsonCodec<?>) codecs.get(itemClass);
                    if (itemCodec == null) {
                        throw missingCodecException(itemClass);
                    }

                    final var wrapper = new CollectionJsonCodec<>(itemCodec, List.class, () -> (Collection) new ArrayList<>());
                    codecs.putIfAbsent(key, wrapper);
                    wrapper.write(collection, new JsonCodec.SerializationContext(writer, this::codecLookup));
                    return;
                }

                writer.write(collection.stream().map(it -> "null").collect(joining(",", "[", "]")));
                return;
            }

            if (instance instanceof Map<?, ?> map) {
                if (map.isEmpty()) {
                    writer.write("{}");
                    return;
                }

                final var entry = map.entrySet().stream()
                        .filter(it -> it.getValue() != null)
                        .findFirst()
                        .orElse(null);
                if (entry != null && entry.getKey() instanceof String && entry.getValue() != null) {
                    if (entry.getValue() instanceof Map<?, ?>) { // consider it is just an object
                        final JsonCodec jsonCodec = codecs.get(Object.class);
                        jsonCodec.write(map, new JsonCodec.SerializationContext(writer, this::codecLookup));
                        return;
                    }

                    final var itemClass = entry.getValue().getClass();
                    // if at least one element does not match the type of the first item don't optimise it and go through object codec
                    if (map.values().stream().filter(Objects::nonNull).anyMatch(it -> !itemClass.isInstance(it))) {
                        final JsonCodec jsonCodec = codecs.get(Object.class);
                        jsonCodec.write(map, new JsonCodec.SerializationContext(writer, this::codecLookup));
                        return;
                    }

                    final var key = new Types.ParameterizedTypeImpl(Map.class, String.class, itemClass);
                    final JsonCodec existing = codecs.get(key);
                    if (existing != null) {
                        existing.write(map, new JsonCodec.SerializationContext(writer, this::codecLookup));
                        return;
                    }

                    final var itemCodec = (JsonCodec<?>) codecs.get(itemClass);
                    if (itemCodec == null) {
                        throw missingCodecException(itemClass);
                    }
                    final var wrapper = new MapJsonCodec<>(itemCodec);
                    codecs.putIfAbsent(key, wrapper);
                    wrapper.write((Map) map, new JsonCodec.SerializationContext(writer, this::codecLookup));
                    return;
                }

                writer.write(map.keySet().stream()
                        .map(it -> '"' + JsonStrings.escape(String.valueOf(it)) + "\":null")
                        .collect(joining(",", "{", "}")));
                return;
            }

            final var clazz = instance.getClass();
            final var codec = (JsonCodec<A>) codecs.get(clazz);
            if (codec == null) {
                throw missingCodecException(clazz);
            }

            codec.write(instance, new JsonCodec.SerializationContext(writer, this::codecLookup));
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A read(final Type type, final Reader rawReader) {
        try (final var reader = parserFactory.apply(rawReader)) {
            final var codec = (JsonCodec<A>) codecs.get(type);
            if (codec == null) {
                if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> rawClass) {
                    if (rawClass == Map.class && pt.getActualTypeArguments().length == 2 && pt.getActualTypeArguments()[0] == String.class) {
                        final var delegate = codecs.get(pt.getActualTypeArguments()[1]);
                        if (delegate == null) {
                            throw missingCodecException(pt.getActualTypeArguments()[0]);
                        }
                        final var wrapper = new MapJsonCodec<>(delegate);
                        codecs.putIfAbsent(wrapper.type(), wrapper);
                        return (A) wrapper.read(new JsonCodec.DeserializationContext(reader, this::codecLookup));
                    }
                    if ((rawClass == List.class || rawClass == Collection.class) && pt.getActualTypeArguments().length == 1) {
                        final var delegate = codecs.get(pt.getActualTypeArguments()[0]);
                        if (delegate == null) {
                            throw missingCodecException(pt.getActualTypeArguments()[0]);
                        }
                        final var wrapper = new CollectionJsonCodec<>(delegate, List.class, ArrayList::new);
                        codecs.putIfAbsent(wrapper.type(), wrapper);
                        return (A) wrapper.read(new JsonCodec.DeserializationContext(reader, this::codecLookup));
                    }
                    if (rawClass == Set.class && pt.getActualTypeArguments().length == 2) {
                        final var delegate = codecs.get(pt.getActualTypeArguments()[0]);
                        if (delegate == null) {
                            throw missingCodecException(pt.getActualTypeArguments()[0]);
                        }
                        final var wrapper = new CollectionJsonCodec<>(delegate, Set.class, HashSet::new);
                        codecs.putIfAbsent(wrapper.type(), wrapper);
                        return (A) wrapper.read(new JsonCodec.DeserializationContext(reader, this::codecLookup));
                    }
                }
                throw missingCodecException(type);
            }

            return codec.read(new JsonCodec.DeserializationContext(reader, this::codecLookup));
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    // just a convenient method for typing
    @Override
    @SuppressWarnings("unchecked")
    public <A> A read(final Class<A> type, final Reader reader) {
        final var codec = (JsonCodec<A>) codecs.get(type);
        if (codec == null) {
            throw missingCodecException(type);
        }
        try (final var jsonReader = parserFactory.apply(reader)) {
            return codec.read(new JsonCodec.DeserializationContext(jsonReader, this::codecLookup));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        final var error = new IllegalStateException("Can't close some codec");
        codecs.values().stream()
                .filter(AutoCloseable.class::isInstance)
                .map(AutoCloseable.class::cast)
                .forEach(close -> {
                    try {
                        close.close();
                    } catch (final Exception e) {
                        error.addSuppressed(e);
                    }
                });
        if (error.getSuppressed().length > 0) {
            throw error;
        }
    }

    private Map<Type, JsonCodec<?>> toCodecMap(final Stream<JsonCodec<?>> codecStream) {
        return codecStream.collect(toMap(JsonCodec::type, identity()));
    }

    private JsonCodec<?> codecLookup(final Class<?> type) {
        return codecs.get(type);
    }

    private IllegalStateException missingCodecException(final Type type) {
        return new IllegalStateException("No codec for '" + type.getTypeName() + "', did you forget to mark it @JsonModel");
    }

    private static Function<Reader, Parser> createReaderParserFunction(final Configuration configuration) {
        final int maxStringLength = configuration.get("comic.json.maxStringLength")
                .map(Integer::parseInt)
                .orElse(64 * 1024);
        final boolean autoAdjust = configuration.get("comic.json.bufferAutoAdjust")
                .map(Boolean::parseBoolean)
                .orElse(true);
        final var bufferFactory = new BufferProvider(maxStringLength);
        return reader -> new JsonParser(reader, maxStringLength, bufferFactory, autoAdjust);
    }

}
