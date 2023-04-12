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
import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.container.bean.BaseBean;
import com.dremio.cloud.comic.processor.internal.Elements;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class BeanConfigurationGenerator extends BaseGenerator implements Supplier<BaseGenerator.GeneratedClass> {

    private final String packageName;
    private final String className;
    private final Element element;

    public BeanConfigurationGenerator(final ProcessingEnvironment processingEnv, final Elements elements,
                                      final String packageName, final String className, final Element element) {
        super(processingEnv, elements);
        this.packageName = packageName;
        this.className = className;
        this.element = element;
    }

    @Override
    public GeneratedClass get() {
        final var pckPrefix = packageName.isBlank() ? "" : (packageName + '.');
        final var simpleName = className + "$RootConfiguration$" + ComicBean.class.getSimpleName();
        final var confBeanClassName = pckPrefix + simpleName;

        final var out = new StringBuilder();
        if (!packageName.isBlank()) {
            out.append("package ").append(packageName).append(";\n\n");
        }
        out.append("public class ").append(simpleName).append(" extends ")
                .append(BaseBean.class.getName()).append("<").append(pckPrefix).append(className.replace('$', '.')).append("> {\n");
        out.append("  public ").append(simpleName).append("() {\n");
        out.append("    super(")
                .append(pckPrefix).append(className.replace('$', '.')).append(".class, ")
                .append(findScope(element)).append(".class, ")
                .append(findPriority(element)).append(", ")
                .append(Map.class.getName()).append(".of());\n");
        out.append("  }\n");
        out.append("\n");
        out.append("  @Override\n");
        out.append("  public ").append(className.replace('$', '.')).append(" create(final ").append(RuntimeContainer.class.getName())
                .append(" container, final ")
                .append(List.class.getName()).append("<").append(Instance.class.getName()).append("<?>> dependents) {\n");
        out.append("    final var conf = lookup(container, ").append(Configuration.class.getName()).append(".class, dependents);\n");
        out.append("    return new ").append(pckPrefix).append(className).append(ConfigurationFactoryGenerator.SUFFIX).append("(conf).get();\n");
        out.append("  }\n");
        out.append("}\n\n");

        return new GeneratedClass(confBeanClassName, out.toString());
    }

}
