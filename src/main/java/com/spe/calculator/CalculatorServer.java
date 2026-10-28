package com.spe.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Minimal REST server built on the JDK's built-in com.sun.net.httpserver (no Spring, no external libraries).
 *
 * Endpoints:
 *   GET /api/v1/{add|subtract|multiply|divide}?a=&b=
 *   GET /actuator/health
 */
public class CalculatorServer {

    private static final Set<String> OPERATIONS = Set.of("add", "subtract", "multiply", "divide");
    private static final String API_PREFIX = "/api/v1/";

    private final Calculator calculator = new Calculator();
    private final HttpServer server;

    public CalculatorServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(API_PREFIX, this::handleCalculation);
        server.createContext("/actuator/health", this::handleHealth);
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    /** Actual bound port; useful when started with port 0 in tests. */
    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        send(exchange, 200, "{\"status\":\"UP\"}");
    }

    private void handleCalculation(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        String operation = exchange.getRequestURI().getPath().substring(API_PREFIX.length());
        if (!OPERATIONS.contains(operation)) {
            sendError(exchange, 404, "Unknown operation: " + operation);
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
        double a;
        double b;
        try {
            a = parseNumber(params, "a");
            b = parseNumber(params, "b");
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
            return;
        }

        double result;
        try {
            result = calculator.apply(operation, a, b);
        } catch (ArithmeticException e) {
            sendError(exchange, 400, e.getMessage());
            return;
        }

        String body = "{\"operation\":\"" + operation + "\","
                + "\"a\":" + formatNumber(a) + ","
                + "\"b\":" + formatNumber(b) + ","
                + "\"result\":" + formatNumber(result) + "}";
        send(exchange, 200, body);
    }

    static double parseNumber(Map<String, String> params, String name) {
        String raw = params.get(name);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required parameter '" + name + "'");
        }
        double value;
        try {
            value = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a number, got '" + raw + "'");
        }
        // Double.parseDouble accepts "NaN" and "Infinity"; those are not valid calculator inputs.
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a finite number");
        }
        return value;
    }

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    /** Prints whole numbers without a trailing ".0" so 2 + 3 returns 5, not 5.0. */
    static String formatNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        send(exchange, status, "{\"status\":" + status + ",\"error\":\"" + escapeJson(message) + "\"}");
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
