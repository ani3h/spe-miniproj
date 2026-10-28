package com.spe.calculator;

/**
 * Pure calculator logic, kept separate from HTTP handling so it can be unit tested directly.
 */
public class Calculator {

    public double add(double a, double b) {
        return a + b;
    }

    public double subtract(double a, double b) {
        return a - b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return a / b;
    }

    /** Dispatches an operation by name, as used in the URL path (/api/v1/{operation}). */
    public double apply(String operation, double a, double b) {
        switch (operation) {
            case "add":
                return add(a, b);
            case "subtract":
                return subtract(a, b);
            case "multiply":
                return multiply(a, b);
            case "divide":
                return divide(a, b);
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }
}
