package com.familygraph.model.validation;

public record ValidationResult(
        boolean valid,
        NormalizedInput data,
        ValidationError error
) {
    public ValidationResult {
        if (valid) {
            if (data == null) {
                throw new IllegalArgumentException("data is required for a valid result");
            }
            if (error != null) {
                throw new IllegalArgumentException("error must be null for a valid result");
            }
        } else {
            if (error == null) {
                throw new IllegalArgumentException("error is required for an invalid result");
            }
            if (data != null) {
                throw new IllegalArgumentException("data must be null for an invalid result");
            }
        }
    }

    public static ValidationResult success(NormalizedInput data) {
        return new ValidationResult(true, data, null);
    }

    public static ValidationResult failure(ValidationError error) {
        return new ValidationResult(false, null, error);
    }
}
