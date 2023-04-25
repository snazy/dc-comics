package com.dremio.cloud.comics.examples.first;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FooBean {

    private final Logger logger = LoggerFactory.getLogger(FooBean.class);

    public void startup(@Observes StartupEvent startupEvent) {
        logger.info("Hello world !");
    }

    public void shutdown(@Observes ShutdownEvent shutdownEvent) {
        logger.info("Bye bye world !");
    }

}
