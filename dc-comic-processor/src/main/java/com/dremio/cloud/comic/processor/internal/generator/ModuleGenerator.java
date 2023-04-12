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
package com.dremio.cloud.comic.processor.internal.generator;

import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.container.ComicListener;
import com.dremio.cloud.comic.api.container.ComicModule;
import com.dremio.cloud.comic.processor.internal.Elements;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class ModuleGenerator extends BaseGenerator implements Supplier<BaseGenerator.GeneratedClass> {

    private final String packageName;
    private final String className;
    private final Collection<String> allBeans;
    private final Collection<String> allListeners;

    public ModuleGenerator(final ProcessingEnvironment processingEnv, final Elements elements,
                           final String packageName, final String className,
                           final Collection<String> allBeans, final Collection<String> allListeners) {
        super(processingEnv, elements);
        this.packageName = packageName;
        this.className = className;
        this.allBeans = allBeans;
        this.allListeners = allListeners;
    }

    @Override
    public GeneratedClass get() {
        final var out = new StringBuilder();

        if (!packageName.isBlank()) {
            out.append("package ").append(packageName).append(";\n");
            out.append("\n");
        }
        out.append("import ").append(Stream.class.getName()).append(";\n");
        out.append("import ").append(ComicBean.class.getName()).append(";\n");
        out.append("import ").append(ComicListener.class.getName()).append(";\n");
        out.append("import ").append(ComicModule.class.getName()).append(";\n");
        out.append("\n");
        out.append("public class ").append(className).append(" implements ").append(ComicModule.class.getSimpleName()).append(" {\n");
        if (!allBeans.isEmpty()) {
            out.append("    @Override\n");
            out.append("    public Stream<FusionBean<?>> beans() {\n");
            out.append("        return Stream.of(\n");
            out.append(allBeans.stream()
                    .sorted()
                    .map(it -> "            new " + it + "()")
                    .collect(joining(",\n", "", "\n")));
            out.append("        );\n");
            out.append("    }\n");
        }
        out.append("\n");
        if (!allListeners.isEmpty()) {
            out.append("    @Override\n");
            out.append("    public Stream<FusionListener<?>> listeners() {\n");
            out.append("        return Stream.of(\n");
            out.append(allListeners.stream()
                    .sorted()
                    .map(it -> "            new " + it + "()")
                    .collect(joining(",\n", "", "\n")));
            out.append("        );\n");
            out.append("    }\n");
        }
        out.append("}\n\n");

        return new GeneratedClass((!packageName.isBlank() ? packageName + '.' : "") + className, out.toString());
    }

}
