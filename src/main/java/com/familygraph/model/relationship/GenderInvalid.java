package com.familygraph.model.relationship;

/** Raised when a queried person has no known gender. */
public final class GenderInvalid extends IllegalArgumentException {
    public GenderInvalid(String personId) {
        super("GenderInvalid: gender is unknown for person " + personId);
    }
}
