/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Function;

public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final Properties PROPS = loadConfigurationFile();
    private static final Map<String, String> CONFIG = new TreeMap<>();

    public static final int STRIMZI_HTTP_PORT = getOrDefault("strimzi.http.port", 8080, Integer::parseInt);
    public static final int STRIMZI_HTTPS_PORT = getOrDefault("strimzi.https.port", 8443, Integer::parseInt);
    public static final String STRIMZI_HTTPS_CRT_PATH = getOrDefault("strimzi.https.crt.path", null);
    public static final String STRIMZI_HTTPS_KEY_PATH = getOrDefault("strimzi.https.key.path", null);
    public static final boolean STRIMZI_DRAIN_ZOOKEEPER = getOrDefault("strimzi.drain.zookeeper", true, Boolean::parseBoolean);
    public static final boolean STRIMZI_DRAIN_KAFKA = getOrDefault("strimzi.drain.kafka", true, Boolean::parseBoolean);
    public static final boolean STRIMZI_DENY_EVICTION = getOrDefault("strimzi.drain.kafka", true, Boolean::parseBoolean);
    public static final boolean STRIMZI_CERTIFICATE_WATCH_ENABLED = getOrDefault("strimzi.certificate.watch.enabled", false, Boolean::parseBoolean);
    public static final String STRIMZI_CERTIFICATE_WATCH_NAMESPACE = getOrDefault("strimzi.certificate.watch.namespace", null);
    public static final String STRIMZI_CERTIFICATE_WATCH_POD_NAME = getOrDefault("strimzi.certificate.watch.pod.name", null);
    public static final String STRIMZI_CERTIFICATE_WATCH_SECRET_NAME = getOrDefault("strimzi.certificate.watch.secret.name", null);
    public static final String STRIMZI_CERTIFICATE_WATCH_SECRET_KEYS = getOrDefault("strimzi.certificate.watch.secret.keys", null);

    private Configuration() {
    }

    static {
        LOG.info("=======================================================");
        CONFIG.forEach((k, v) -> LOG.info("{}: {}", k,
            k.toLowerCase(Locale.ROOT).contains("password") && v != null ? "*****" : v));
        LOG.info("=======================================================");
    }

    private static Properties loadConfigurationFile() {
        Properties prop = new Properties();
        try {
            prop.load(Configuration.class.getClassLoader().getResourceAsStream("application.properties"));
            return prop;
        } catch (IOException e) {
            throw new RuntimeException("Load configuration error", e);
        }
    }

    private static String getOrDefault(String key, String defaultValue) {
        return getOrDefault(key, defaultValue, String::toString);
    }

    private static <T> T getOrDefault(String key, T defaultValue, Function<String, T> converter) {
        String envKey = key != null ? key.toUpperCase(Locale.ENGLISH).replaceAll("\\.", "_") : null;
        String value = System.getenv(envKey) != null ? System.getenv(envKey) :
            (Objects.requireNonNull(PROPS).get(key) != null ? PROPS.getProperty(key) : null);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        CONFIG.put(key, String.valueOf(returnValue));
        return returnValue;
    }
}
