package com.spe.calculator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: starts the real HTTP server on a random free port and calls it over HTTP.
 */
class CalculatorServerTest {

    private static CalculatorServer server;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        server = new CalculatorServer(0);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void add() throws Exception {
        HttpResponse<String> res = get("/api/v1/add?a=2&b=3");
        assertEquals(200, res.statusCode());
        assertEquals("{\"operation\":\"add\",\"a\":2,\"b\":3,\"result\":5}", res.body());
        assertEquals("application/json", res.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void subtract() throws Exception {
        HttpResponse<String> res = get("/api/v1/subtract?a=10&b=4.5");
        assertEquals(200, res.statusCode());
        assertEquals("{\"operation\":\"subtract\",\"a\":10,\"b\":4.5,\"result\":5.5}", res.body());
    }

    @Test
    void multiply() throws Exception {
        HttpResponse<String> res = get("/api/v1/multiply?a=-2&b=3");
        assertEquals(200, res.statusCode());
        assertEquals("{\"operation\":\"multiply\",\"a\":-2,\"b\":3,\"result\":-6}", res.body());
    }

    @Test
    void divide() throws Exception {
        HttpResponse<String> res = get("/api/v1/divide?a=5&b=2");
        assertEquals(200, res.statusCode());
        assertEquals("{\"operation\":\"divide\",\"a\":5,\"b\":2,\"result\":2.5}", res.body());
    }

    @Test
    void divideByZeroReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/divide?a=5&b=0");
        assertEquals(400, res.statusCode());
        assertEquals("{\"status\":400,\"error\":\"Division by zero is not allowed\"}", res.body());
    }

    @Test
    void missingParameterReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/add?a=5");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("Missing required parameter 'b'"));
    }

    @Test
    void nonNumericParameterReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/add?a=abc&b=1");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("must be a number"));
    }

    @Test
    void nonFiniteParameterReturns400() throws Exception {
        HttpResponse<String> res = get("/api/v1/add?a=NaN&b=1");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("finite"));
    }

    @Test
    void unknownOperationReturns404() throws Exception {
        assertEquals(404, get("/api/v1/power?a=2&b=3").statusCode());
    }

    @Test
    void healthIsUp() throws Exception {
        HttpResponse<String> res = get("/actuator/health");
        assertEquals(200, res.statusCode());
        assertEquals("{\"status\":\"UP\"}", res.body());
    }
}
