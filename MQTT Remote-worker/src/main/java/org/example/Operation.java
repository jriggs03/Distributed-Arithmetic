package org.example;

import java.util.Locale;

public class Operation {

    private final String id;
    private final int left;
    private final char operator;
    private final int right;

    public Operation(String id, int left, char operator, int right) {
        this.id = id;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public double solve() {
        switch (operator) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                return (double) left / right;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    public String getExpression() {
        return left + " " + operator + " " + right;
    }

    public String getResultText() {
        return String.format(Locale.US, "%.2f", solve());
    }
    public String getId() {
        return id;
    }
    public int getLeft() {
        return left;
    }
    public char getOperator() {
        return operator;
    }
    public int getRight() {
        return right;
    }
}