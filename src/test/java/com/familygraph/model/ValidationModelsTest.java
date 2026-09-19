package com.familygraph.model;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.validation.NormalizedInput;
import com.familygraph.model.validation.ValidationError;
import com.familygraph.model.validation.ValidationErrorCode;
import com.familygraph.model.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationModelsTest {

    @Test
    void createsConsistentSuccessAndFailureResults() {
        NormalizedInput data = new NormalizedInput(
                new FamilyGraph(List.of(new Person("P01", Gender.UNKNOWN, "")), List.of()),
                new FamilyTreeQuery("P01", 1, Direction.BOTH)
        );
        ValidationError error = new ValidationError(
                ValidationErrorCode.UNKNOWN_ID,
                "P99",
                List.of()
        );

        ValidationResult success = ValidationResult.success(data);
        ValidationResult failure = ValidationResult.failure(error);

        assertTrue(success.valid());
        assertSame(data, success.data());
        assertNull(success.error());
        assertFalse(failure.valid());
        assertNull(failure.data());
        assertSame(error, failure.error());
    }

    @Test
    void rejectsContradictoryValidationStates() {
        ValidationError error = new ValidationError(
                ValidationErrorCode.MALFORMED_INPUT,
                "",
                List.of()
        );

        assertThrows(IllegalArgumentException.class,
                () -> new ValidationResult(true, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationResult(true, null, error));
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationResult(false, null, null));
    }

    @Test
    void onlyDirectedCycleErrorMayContainAClosedCycle() {
        ValidationError cycleError = new ValidationError(
                ValidationErrorCode.DIRECTED_CYCLE,
                "",
                List.of("P01", "P02", "P01")
        );

        assertSame(ValidationErrorCode.DIRECTED_CYCLE, cycleError.code());
        assertThrows(IllegalArgumentException.class,
                () -> new ValidationError(
                        ValidationErrorCode.UNKNOWN_ID,
                        "P99",
                        List.of("P01", "P02", "P01")
                ));
    }
}
