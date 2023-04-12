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

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.container.bean.BaseBean;
import com.dremio.cloud.comic.api.scope.DefaultScoped;
import com.dremio.cloud.comic.processor.internal.Elements;

import javax.annotation.processing.ProcessingEnvironment;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class JsonCodecBeanGenerator extends BaseGenerator implements Supplier<BaseGenerator.GeneratedClass> {

    private final String packageName;
    private final String className;

    public JsonCodecBeanGenerator(final ProcessingEnvironment processingEnv, final Elements elements,
                                  final String packageName, final String className) {
        super(processingEnv, elements);
        this.packageName = packageName;
        this.className = className;
    }

    @Override
    public GeneratedClass get() {
        final var pckPrefix = packageName.isBlank() ? "" : (packageName + '.');
        final var simpleName = className + "$" + ComicBean.class.getSimpleName();
        final var confBeanClassName = pckPrefix + simpleName;

        final var out = new StringBuilder();
        if (!packageName.isBlank()) {
            out.append("package ").append(packageName).append(";\n\n");
        }
        out.append("public class ").append(simpleName).append(" extends ")
                .append(BaseBean.class.getName()).append("<").append(pckPrefix).append(className).append("> {\n");
        out.append("  public ").append(className).append('$').append(ComicBean.class.getSimpleName()).append("() {\n");
        out.append("    super(")
                .append(className).append(".class, ")
                .append(DefaultScoped.class.getName()).append(".class, ") // will be a singleton in json mapper anyway
                .append("1000, ")
                .append(Map.class.getName()).append(".of());\n");
        out.append("  }\n");
        out.append("\n");
        out.append("  @Override\n");
        out.append("  public ").append(className).append(" create(final ").append(RuntimeContainer.class.getName())
                .append(" container, final ")
                .append(List.class.getName()).append("<").append(Instance.class.getName()).append("<?>> dependents) {\n");
        out.append("    return new ").append(pckPrefix).append(className).append("();\n");
        out.append("  }\n");
        out.append("}\n\n");

        return new GeneratedClass(confBeanClassName, out.toString());
    }


}
