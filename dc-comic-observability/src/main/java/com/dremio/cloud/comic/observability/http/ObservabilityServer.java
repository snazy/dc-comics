package com.dremio.cloud.comic.observability.http;

import com.dremio.cloud.comic.api.configuration.Configuration;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.build.api.event.OnEvent;
import com.dremio.cloud.comic.http.server.api.WebServer;
import com.dremio.cloud.comic.http.server.impl.servlet.ComicServlet;
import com.dremio.cloud.comic.http.server.impl.tomcat.TomcatWebServer;
import com.dremio.cloud.comic.http.server.impl.tomcat.TomcatWebServerConfiguration;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;

import java.util.Set;
import java.util.stream.Stream;

import static com.dremio.cloud.comic.http.server.impl.tomcat.TomcatWebServer.createBaseContext;

@ApplicationScoped
public class ObservabilityServer {

    private final Configuration configuration;
    private final MonitoringEndpointRegistry registry;
    private int port = -1;

    protected ObservabilityServer() {
        this(null, null);
    }

    public ObservabilityServer(final Configuration configuration, final MonitoringEndpointRegistry registry) {
        this.configuration = configuration;
        this.registry = registry;
    }

    public int getPort() {
        return port;
    }

    public void onWebServerConfiguration(@OnEvent final WebServer.Configuration configuration) {
        final var tomcatWebServerConfiguration = configuration.unwrap(TomcatWebServerConfiguration.class);
        final var customizers = tomcatWebServerConfiguration.getTomcatCustomizers();
        tomcatWebServerConfiguration.setTomcatCustomizers(
                Stream.concat(
                                customizers != null ? customizers.stream() : Stream.empty(),
                                Stream.of(t -> addObservabilityServer(t, tomcatWebServerConfiguration)))
                        .toList());
    }

    protected void addObservabilityServer(final Tomcat tomcat, final TomcatWebServerConfiguration webConf) {
        final var host = new StandardHost();
        host.setAutoDeploy(false);
        host.setName("localhost");
        host.addChild(newContext(webConf));

        final var engine = new StandardEngine();
        engine.setName("Monitoring");
        engine.setDefaultHost(host.getName());
        engine.addChild(host);

        final var connector = new Connector() {
            @Override
            protected void startInternal() throws LifecycleException {
                super.startInternal();
                if (getProtocolHandler() instanceof AbstractProtocol<?> ap) {
                    port = ap.getLocalPort();
                }
            }
        };
        connector.setPort(this.configuration.get("comic.observability.server.port")
                .map(Integer::parseInt)
                .orElse(8181));

        final var service = new StandardService();
        service.setName("Observability");
        service.addConnector(connector);
        service.setContainer(engine);

        tomcat.getServer().addService(service);
    }

    protected Context newContext(final TomcatWebServerConfiguration webConf) {
        final var baseContext = createBaseContext(new TomcatWebServer.NoWorkDirContext(), webConf);
        baseContext.addServletContainerInitializer((ignored, ctx) -> {
            final var observability = ctx.addServlet("observability", new ComicServlet(registry.endpoints()));
            observability.setAsyncSupported(true);
            observability.setLoadOnStartup(1);
            observability.addMapping("/*");
        }, Set.of());
        return baseContext;
    }

}
