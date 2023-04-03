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
package com.dremio.cloud.comic.httpclient.core.listener.impl.har;

import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.json.internal.JsonMapperImpl;
import com.dremio.cloud.comic.json.internal.JsonStrings;
import com.dremio.cloud.comic.json.serialization.JsonCodec;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.dremio.cloud.comic.json.spi.Parser.Event.VALUE_STRING;

public class HarJsonMapper extends JsonMapperImpl {

    public HarJsonMapper() {
        super(
                List.of(
                        new BaseHARDumperListener$Har$Cache$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Header$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$PostData$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$CacheRequest$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Identity$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Query$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Content$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Log$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Request$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Cookie$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Page$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Response$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Entry$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$PageTiming$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Timings$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$FusionJsonCodec(),
                        new BaseHARDumperListener$Har$Param$FusionJsonCodec()),
                Configuration.of(Map.of()));
    }

    @Override
    protected Stream<JsonCodec<?>> builtInCodecs() {
        return Stream.concat(
                super.builtInCodecs()
                        .filter(it -> it.type() != ZonedDateTime.class),
                Stream.of(new HarZonedDateTimeJsonCodec()));
    }

    private static class HarZonedDateTimeJsonCodec implements JsonCodec<ZonedDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        @Override
        public Type type() {
            return ZonedDateTime.class;
        }

        @Override
        public ZonedDateTime read(final DeserializationContext context) {
            final var parser = context.parser();
            if (!parser.hasNext() || parser.next() != VALUE_STRING) {
                throw new IllegalStateException("Expected VALUE_STRING");
            }
            return ZonedDateTime.parse(parser.getString(), FORMATTER);
        }

        @Override
        public void write(final ZonedDateTime value, final SerializationContext context) throws IOException {
            context.writer().write(JsonStrings.escape(value.format(FORMATTER)));
        }
    }

}
