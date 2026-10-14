package com.familygraph.input;

import com.familygraph.input.model.GraphValidationResult;
import com.familygraph.input.model.ParseResult;
import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedInput;
import com.familygraph.input.model.ParsedPerson;
import com.familygraph.input.model.ParsedQuery;
import com.familygraph.input.model.ValidatedInput;
import com.familygraph.model.validation.ValidationError;
import com.familygraph.model.validation.ValidationErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InputPipelineModelsTest {
    private final ParsedInput input = new ParsedInput(
            new ParsedFamilyGraph(1,
                    List.of(new ParsedPerson("P01", "MALE", "")), 0, List.of()),
            new ParsedQuery("FAMILY_TREE", List.of("P01", "1", "BOTH"))
    );
    private final ValidationError error = new ValidationError(
            ValidationErrorCode.INVALID_COUNT, "test", List.of()
    );

    @Test
    void parseResultRejectsContradictoryStates() {
        assertThrows(IllegalArgumentException.class,
                () -> new ParseResult(true, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ParseResult(false, input, error));
    }

    @Test
    void graphValidationResultRejectsContradictoryStates() {
        ValidatedInput validated = new ValidatedInput(input);
        assertThrows(IllegalArgumentException.class,
                () -> new GraphValidationResult(true, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new GraphValidationResult(false, validated, error));
    }
}
