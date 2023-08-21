package org.wiremock.integrations.testcontainers.util;

/*
 * @deprecated this class is used only for the Java 8 which has no embedded client.
 *             It is not a part of public API and will be removed at any moment.
 */
@Deprecated
public class SimpleHttpResponse {

    String body;
    int statusCode;

    public SimpleHttpResponse(String body, int statusCode) {
        this.body = body;
        this.statusCode = statusCode;
    }

    public String getBody() {
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
