/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DrainCleaner entrypoint.
 */
public class Main {
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
     * Starts the webhook handler and certificate watch.
     *
     * @param args Startup arguments
     */
    public static void main(String[] args) {
        LOG.info("DrainCleaner {} is starting", Main.class.getPackage().getImplementationVersion());
        KubernetesClient kubeClient = new KubernetesClientBuilder().build();

        HttpServer httpServer = new HttpServer(kubeClient);
        CertificateWatch certificateWatch = new CertificateWatch(kubeClient);

        httpServer.start();
        certificateWatch.start();

        LOG.debug("Registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("DrainCleaner shutdown");
            certificateWatch.stop();
            httpServer.stop();
            kubeClient.close();
            LOG.info("Shutdown complete");
        }));
    }
}
