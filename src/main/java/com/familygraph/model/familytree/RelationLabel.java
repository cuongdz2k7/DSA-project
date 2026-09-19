package com.familygraph.model.familytree;

import java.util.Objects;

public record RelationLabel(
        RelationType type,
        int greatDegree
) {
    public RelationLabel {
        Objects.requireNonNull(type, "type must not be null");
        if (isGreatType(type)) {
            if (greatDegree < 1) {
                throw new IllegalArgumentException("greatDegree must be at least 1 for GREAT relations");
            }
        } else if (greatDegree != 0) {
            throw new IllegalArgumentException("greatDegree must be 0 for non-GREAT relations");
        }
    }

    public String code() {
        return isGreatType(type) ? type.name() + "_" + greatDegree : type.name();
    }

    public int generationOffset() {
        return switch (type) {
            case SELF -> 0;
            case FATHER, MOTHER, PARENT -> 1;
            case GRANDFATHER, GRANDMOTHER, GRANDPARENT -> 2;
            case GREAT_GRANDFATHER, GREAT_GRANDMOTHER, GREAT_GRANDPARENT -> greatDegree + 2;
            case SON, DAUGHTER, CHILD -> -1;
            case GRANDSON, GRANDDAUGHTER, GRANDCHILD -> -2;
            case GREAT_GRANDSON, GREAT_GRANDDAUGHTER, GREAT_GRANDCHILD -> -(greatDegree + 2);
        };
    }

    private static boolean isGreatType(RelationType type) {
        return switch (type) {
            case GREAT_GRANDFATHER,
                    GREAT_GRANDMOTHER,
                    GREAT_GRANDPARENT,
                    GREAT_GRANDSON,
                    GREAT_GRANDDAUGHTER,
                    GREAT_GRANDCHILD -> true;
            default -> false;
        };
    }
}
