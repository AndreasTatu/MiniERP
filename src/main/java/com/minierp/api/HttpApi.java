package com.minierp.api;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

final class HttpApi {
    private HttpApi() {}
    private static final int MAX_BODY_BYTES = 65_536;
    static final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

    static <T> T body(HttpExchange exchange, Class<T> type) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new HttpProblem(415, "Content-Type muss application/json sein.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) throw new HttpProblem(413, "Die Anfrage ist zu groß.");
        T request = json.readValue(bytes, type);
        if (request == null) throw new IllegalArgumentException("Die Anfrage darf nicht leer sein.");
        return request;
    }

    static void requireMethod(HttpExchange exchange, String... allowed) {
        for (String method : allowed) if (exchange.getRequestMethod().equals(method)) return;
        exchange.getResponseHeaders().set("Allow", String.join(", ", allowed) + ", OPTIONS");
        throw new HttpProblem(405, "HTTP-Methode wird nicht unterstützt.");
    }

    record Response(int status, Object body, String location) {}
    static final class HttpProblem extends RuntimeException {
        final int status;
        HttpProblem(int status, String message) { super(message); this.status = status; }
    }


}
