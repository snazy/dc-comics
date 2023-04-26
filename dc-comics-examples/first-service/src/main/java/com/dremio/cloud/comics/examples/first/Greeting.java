package com.dremio.cloud.comics.examples.first;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Greeting {

    @Inject
    Tracer tracer;

    private final Logger logger = LoggerFactory.getLogger(Greeting.class);

    public void startup(@Observes StartupEvent startupEvent) {
        Span span = tracer.spanBuilder("My custom span")
                        .setAttribute("my.attr", "attr")
                                .setParent(Context.current().with(Span.current()))
                                        .setSpanKind(SpanKind.INTERNAL)
                                                .startSpan();
        span.addEvent("Starting ...");
        logger.info("Hello world !");
        span.end();
    }

    @WithSpan
    public void shutdown(@Observes ShutdownEvent shutdownEvent) {
        logger.info("Bye bye world !");
    }

}
