/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HealthHandlerTest {
    @Test
    public void testEndpoint() throws URISyntaxException, IOException, InterruptedException {
        HttpServer httpServer = new HttpServer(new KubernetesClientBuilder().build());
        httpServer.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI("http://localhost:8080/health")).GET().build();
            sendAndExpect(client, request, 200, "{\"status\":\"RUNNING\"}");
        } finally {
            httpServer.stop();
        }
    }

    /**
     * Send an HTTP request and assert the response.
     *
     * @param client         HTTP client
     * @param request        HTTP request
     * @param expectedStatus Expected status
     * @param expectedBody   Expected response body
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendAndExpect(HttpClient client, HttpRequest request, int expectedStatus, String expectedBody) throws IOException, InterruptedException {
        HttpResponse<String> response =  client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode(), is(expectedStatus));
        assertThat(response.body().trim(), is(expectedBody));
    }
}
