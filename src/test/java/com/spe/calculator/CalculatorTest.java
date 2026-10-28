package com.spe.calculator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    void addsTwoNumbers() {
        assertEquals(5, calculator.add(2, 3));
        assertEquals(-1.5, calculator.add(-3, 1.5));
    }

    @Test
    void subtractsTwoNumbers() {
        assertEquals(-1, calculator.subtract(2, 3));
        assertEquals(10, calculator.subtract(7, -3));
    }

    @Test
    void multipliesTwoNumbers() {
        assertEquals(6, calculator.multiply(2, 3));
        assertEquals(0, calculator.multiply(0, 99));
        assertEquals(-12, calculator.multiply(-4, 3));
    }

    @Test
    void dividesTwoNumbers() {
        assertEquals(2.5, calculator.divide(5, 2));
        assertEquals(-3, calculator.divide(9, -3));
    }

    @Test
    void divideByZeroThrows() {
        ArithmeticException e = assertThrows(ArithmeticException.class, () -> calculator.divide(1, 0));
        assertEquals("Division by zero is not allowed", e.getMessage());
    }

    @Test
    void applyDispatchesByName() {
        assertEquals(5, calculator.apply("add", 2, 3));
        assertEquals(-1, calculator.apply("subtract", 2, 3));
        assertEquals(6, calculator.apply("multiply", 2, 3));
        assertEquals(2, calculator.apply("divide", 6, 3));
    }

    @Test
    void applyRejectsUnknownOperation() {
        assertThrows(IllegalArgumentException.class, () -> calculator.apply("power", 2, 3));
    }
}
