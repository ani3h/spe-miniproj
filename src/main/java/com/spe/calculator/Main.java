package com.spe.calculator;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        CalculatorServer server = new CalculatorServer(port);
        server.start();
        System.out.println("Calculator API listening on port " + server.getPort());

        // Stop cleanly on `docker stop` (SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down calculator API");
            server.stop();
        }));
    }
}
