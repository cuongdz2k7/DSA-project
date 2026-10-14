package com.familygraph.input.model;

import java.util.Objects;

/** Marker object proving that a ParsedInput passed ParsedInputValidator. */
public record ValidatedInput(ParsedInput parsedInput) {
    public ValidatedInput {
        Objects.requireNonNull(parsedInput, "parsedInput must not be null");
    }
}
