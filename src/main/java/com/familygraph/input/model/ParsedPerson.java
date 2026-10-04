package com.familygraph.input.model;

/** Person data read from text before domain validation. */
public record ParsedPerson(
        String id,
        String genderToken,
        String name
) {
}
