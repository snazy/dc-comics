package com.dremio.cloud.comic.processor.internal.generator;

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.container.ComicBean;
import com.dremio.cloud.comic.api.container.bean.BaseBean;
import com.dremio.cloud.comic.build.api.http.HttpMatcher;
import com.dremio.cloud.comic.http.server.api.Request;
import com.dremio.cloud.comic.http.server.api.Response;
import com.dremio.cloud.comic.http.server.impl.DefaultEndpoint;
import com.dremio.cloud.comic.http.server.impl.matcher.CaseInsensitiveValuesMatcher;
import com.dremio.cloud.comic.http.server.impl.matcher.PatternMatcher;
import com.dremio.cloud.comic.http.server.impl.matcher.ValueMatcher;
import com.dremio.cloud.comic.processor.internal.Elements;
import com.dremio.cloud.comic.processor.internal.ParsedType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class HttpEndpointGenerator extends BaseHttpEndpointGenerator implements Supplier<BaseHttpEndpointGenerator.Generation> {

    private static final String SUFFIX = "$ComicHttpEndpoint";

    public HttpEndpointGenerator(final ProcessingEnvironment processingEnv, final Elements elements,
                                 final boolean generateBean, final String packageName, final String className,
                                 final ExecutableElement method, final Set<String> knownJsonModels) {
        super(processingEnv, elements, generateBean, packageName, className, method, knownJsonModels);
    }

    @Override
    public Generation get() {
        final var packagePrefix = packageName == null || packageName.isBlank() ? "" : (packageName + '.');
        final var packageLine = packagePrefix.isBlank() ? "" : ("package " + packageName + ";\n\n");
        final var matcher = method.getAnnotation(HttpMatcher.class);
        final var endpointClassName = className + SUFFIX;

        // drop the method name to get the enclosing one
        final var enclosingClassName = className.substring(0, className.lastIndexOf('$')).replace('$', '.');
        final var returnType = ParsedType.of(method.getReturnType());
        final var params = prepareParams();
        final boolean isReturnTypeJson = isJson(returnType);

        return new Generation(
                new GeneratedClass(packagePrefix + endpointClassName, packageLine +
                        generationVersion() +
                        "public class " + endpointClassName + " extends " + DefaultEndpoint.class.getName() + " {\n" +
                        declareConstructor(endpointClassName, enclosingClassName, params, isReturnTypeJson) +
                        "    super(\n" +
                        "      " + matcher.priority() + ",\n" +
                        "      " + matcherOf(matcher) + ",\n" +
                        "      " + handler(params, returnType, isReturnTypeJson) + ");\n" +
                        "  }\n" +
                        "}\n" +
                        "\n"),
                generateBean ?
                        new GeneratedClass(packagePrefix + endpointClassName + '$' + ComicBean.class.getSimpleName(), packageLine +
                                generationVersion() +
                                "public class " + endpointClassName + '$' + ComicBean.class.getSimpleName() + " extends " + BaseBean.class.getName() + "<" + endpointClassName + "> {\n" +
                                "  public " + endpointClassName + '$' + ComicBean.class.getSimpleName() + "() {\n" +
                                "    super(\n" +
                                "      " + endpointClassName + ".class,\n" +
                                "      " + findScope(method) + ".class,\n" +
                                "      " + findPriority(method) + ",\n" +
                                "      " + Map.class.getName() + ".of());\n" +
                                "  }\n" +
                                "\n" +
                                "  @Override\n" +
                                "  public " + endpointClassName + " create(final " + RuntimeContainer.class.getName() + " container, final " +
                                List.class.getName() + "<" + Instance.class.getName() + "<?>> dependents) {\n" +
                                createBeanInstance(isReturnTypeJson, endpointClassName, enclosingClassName, params) +
                                "  }\n" +
                                "}\n" +
                                "\n") :
                        null);
    }

    private String handler(final List<Param> params, final ParsedType returnType, final boolean returnJson) {
        final var asyncParams = params.stream().filter(p -> p.promiseName() != null).toList();
        final var out = new StringBuilder();
        int indent = 0;
        for (final var param : asyncParams) {
            out.append((param.invocation() + "\n          .thenCompose(" + param.promiseName() + " ->\n").indent(indent));
            indent += 2;
        }

        out.append(forceCompletionStageResult(returnType, "" +
                "root." + method.getSimpleName() + "(" +
                params.stream()
                        .map(it -> it.promiseName() != null ? it.promiseName() : it.invocation())
                        .collect(joining(", ")) +
                ")").indent(indent + "          ".length()).stripTrailing());

        if (!asyncParams.isEmpty()) {
            out.append(")".repeat(asyncParams.size()));
        }

        return "request -> " + out +
                (!returnJson ? "" : "\n" +
                        "          .thenApply(jsonResult -> " + Response.class.getName() + ".of()\n" +
                        "            .status(200)\n" +
                        "            .header(\"content-type\", \"application/json;charset=utf-8\")\n" +
                        "            .body(jsonMapper.toString(jsonResult))\n" +
                        "            .build())");
    }

    private String matcherOf(final HttpMatcher matcher) {
        final var matchers = Stream.of(
                        // optimised method matcher
                        switch (matcher.methods().length) {
                            case 0 -> null;
                            case 1 -> "new " + ValueMatcher.class.getName() + "<>(" +
                                    Request.class.getName() + "::method, \"" + matcher.methods()[0] + "\", String::equalsIgnoreCase)";
                            default -> "new " + CaseInsensitiveValuesMatcher.class.getName() + "<>(" +
                                    Request.class.getName() + "::method, " +
                                    Stream.of(matcher.methods()).map(it -> "\"" + it + "\"").collect(joining(", ")) + ")";
                        },
                        // optimised path matcher
                        switch (matcher.pathMatching()) {
                            case IGNORED -> null;
                            case EXACT -> "new " + ValueMatcher.class.getName() + "<>(" +
                                    Request.class.getName() + "::path, \"" + matcher.path() + "\")";
                            case REGEX -> "new " + PatternMatcher.class.getName() + "<>(" +
                                    Request.class.getName() + "::path,  \"" + matcher.path() + "\", (req, matcher) -> req.setAttribute(\"fusion.http.matcher\", matcher))";
                            case STARTS_WITH -> "new " + ValueMatcher.class.getName() + "<>(" +
                                    Request.class.getName() + "::path, \"" + matcher.path() + "\", String::startsWith)";
                            case ENDS_WITH -> "new " + ValueMatcher.class.getName() + "<>(" +
                                    Request.class.getName() + "::path, \"" + matcher.path() + "\", String::endsWith)";
                        })
                .filter(Objects::nonNull)
                .toList();
        return matchers.isEmpty() ?
                "request -> true" :
                matchers.stream().collect(joining(")\n          .and(", "(", ")"));
    }


}
