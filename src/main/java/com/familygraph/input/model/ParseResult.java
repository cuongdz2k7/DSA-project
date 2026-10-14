package com.familygraph.input.model;

import com.familygraph.model.validation.ValidationError;

import java.util.Objects;

/** Result of syntax parsing. Exactly one of data and error is present. */
public record ParseResult(
        boolean successful,
        ParsedInput data,
        ValidationError error
) {
    public ParseResult {
        if (successful == (data == null)) {
            throw new IllegalArgumentException("successful parse must contain data only");
        }
        if (successful == (error != null)) {
            throw new IllegalArgumentException("failed parse must contain error only");
        }
    }

    public static ParseResult success(ParsedInput data) {
        return new ParseResult(true, Objects.requireNonNull(data), null);
    }

    public static ParseResult failure(ValidationError error) {
        return new ParseResult(false, null, Objects.requireNonNull(error));
    }
}
