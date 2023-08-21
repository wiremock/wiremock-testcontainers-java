package org.wiremock.integrations.testcontainers.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @deprecated this class is used only for the Java 8 which has no embedded client.
 *             It is not a part of public API and will be removed at any moment.
 */
@Deprecated
public final class SimpleHttpClient {

    public SimpleHttpResponse send(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return new SimpleHttpResponse(response.toString(), connection.getResponseCode());
    }

    public SimpleHttpResponse get(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        return send(connection);
    }


    public SimpleHttpResponse post(String uri, String body) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);

        try (OutputStream outputStream = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }

        return send(connection);
    }
}
