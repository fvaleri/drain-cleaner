/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

/**
 * Jetty based HTTP server used for health checks and webhook requests.
 */
public class HttpServer {
    private final static Logger LOG = LoggerFactory.getLogger(HttpServer.class);
    private final static String KEY_STORE_PATH = "/tmp/keystore.p12";
    private final static String KEY_STORE_PASS = UUID.randomUUID().toString();

    private Server server;

    /**
     * Constructs the HTTP server.
     *
     * @param client KubernetesClient client.
     */
    public HttpServer(KubernetesClient client) {
        server = new Server();

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSecureScheme("https");
        httpConfiguration.setSecurePort(Configuration.STRIMZI_HTTPS_PORT);

        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
        http.setPort(Configuration.STRIMZI_HTTP_PORT);
        server.addConnector(http);

        if (fileExists(Configuration.STRIMZI_HTTPS_CRT_PATH)
                && fileExists(Configuration.STRIMZI_HTTPS_KEY_PATH)) {
            try {
                KeyStoreBuilder.build(
                    Configuration.STRIMZI_HTTPS_CRT_PATH,
                    Configuration.STRIMZI_HTTPS_KEY_PATH,
                    KEY_STORE_PATH, KEY_STORE_PASS
                );
            } catch (Exception e) {
                throw new RuntimeException("Keystore build failed", e);
            }

            SslContextFactory sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(KEY_STORE_PATH);
            sslContextFactory.setKeyStorePassword(KEY_STORE_PASS);
            HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
            ServerConnector httpsConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfiguration));
            httpsConnector.setPort(Configuration.STRIMZI_HTTPS_PORT);
            server.addConnector(httpsConnector);
        }

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.addHandler(contextHandler("/health", new HealthHandler()));
        contexts.addHandler(contextHandler("/drainer", new WebhookHandler(client)));
        server.setHandler(contexts);
    }

    private static ContextHandler contextHandler(String path, Handler handler) {
        LOG.debug("Configuring path {} with handler {}", path, handler);
        ContextHandler metricsContext = new ContextHandler();
        metricsContext.setContextPath(path);
        metricsContext.setHandler(handler);
        metricsContext.setAllowNullPathInfo(true);
        return metricsContext;
    }

    private static boolean fileExists(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        File f = new File(path);
        return f.exists() && !f.isDirectory();
    }

    /**
     * Starts the HTTP server.
     */
    public void start() {
        try {
            server.start();
        } catch (Exception e)   {
            LOG.error("Failed to start the health check and metrics webserver", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Stops the HTTP server.
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e)   {
            LOG.error("Failed to stop the health check and metrics webserver", e);
            throw new RuntimeException(e);
        }
    }
}
