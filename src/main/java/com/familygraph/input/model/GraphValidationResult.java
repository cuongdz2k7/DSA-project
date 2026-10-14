package com.familygraph.input.model;

import com.familygraph.model.validation.ValidationError;

import java.util.Objects;

/** Result of semantic graph and query validation. */
public record GraphValidationResult(
        boolean valid,
        ValidatedInput data,
        ValidationError error
) {
    public GraphValidationResult {
        if (valid == (data == null)) {
            throw new IllegalArgumentException("valid result must contain data only");
        }
        if (valid == (error != null)) {
            throw new IllegalArgumentException("invalid result must contain error only");
        }
    }

    public static GraphValidationResult success(ValidatedInput data) {
        return new GraphValidationResult(true, Objects.requireNonNull(data), null);
    }

    public static GraphValidationResult failure(ValidationError error) {
        return new GraphValidationResult(false, null, Objects.requireNonNull(error));
    }
}
