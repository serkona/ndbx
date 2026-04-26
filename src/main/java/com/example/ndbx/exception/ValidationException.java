package com.example.ndbx.exception;

public class ValidationException extends RuntimeException {
    private final String field;

    public ValidationException(String field) {
        super("Validation failed for field: " + field);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
