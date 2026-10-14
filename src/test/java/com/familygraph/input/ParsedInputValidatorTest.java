package com.familygraph.input;

import com.familygraph.input.model.GraphValidationResult;
import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedInput;
import com.familygraph.input.model.ParsedParentChildEdge;
import com.familygraph.input.model.ParsedPerson;
import com.familygraph.input.model.ParsedQuery;
import com.familygraph.model.validation.ValidationErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedInputValidatorTest {
    private final ParsedInputValidator validator = new ParsedInputValidator();

    @Test
    void acceptsMaleFemaleMaleUnknownAndTwoUnknownParentCombinations() {
        assertTrue(validator.validate(input(
                List.of(
                        person("M1", "MALE"), person("F1", "FEMALE"),
                        person("U1", "UNKNOWN"), person("U2", "UNKNOWN"),
                        person("C1", "UNKNOWN"), person("C2", "UNKNOWN"),
                        person("C3", "UNKNOWN"), person("C4", "UNKNOWN")
                ),
                List.of(
                        edge("M1", "C1"), edge("F1", "C1"),
                        edge("M1", "C2"), edge("U1", "C2"),
                        edge("U1", "C3"), edge("U2", "C3"),
                        edge("M1", "C4"), edge("F1", "C4"), edge("U1", "C4")
                ),
                familyQuery("C1", "2", "ANCESTORS")
        )).valid());
    }

    @Test
    void rejectsTwoMaleParentsWithStableDetail() {
        GraphValidationResult result = validator.validate(input(
                List.of(person("P01", "MALE"), person("P02", "MALE"), person("P03", "FEMALE")),
                List.of(edge("P01", "P03"), edge("P02", "P03")),
                familyQuery("P03", "2", "ANCESTORS")
        ));

        assertError(result, ValidationErrorCode.SAME_GENDER_PARENTS);
        assertEquals("child=P03 parents=P01,P02 gender=MALE", result.error().detail());
    }

    @Test
    void rejectsTwoFemaleParents() {
        GraphValidationResult result = validator.validate(input(
                List.of(person("P01", "FEMALE"), person("P02", "FEMALE"), person("P03", "MALE")),
                List.of(edge("P01", "P03"), edge("P02", "P03")),
                familyQuery("P03", "2", "ANCESTORS")
        ));
        assertError(result, ValidationErrorCode.SAME_GENDER_PARENTS);
    }

    @Test
    void duplicateEdgeHasPriorityOverParentGenderError() {
        GraphValidationResult result = validator.validate(input(
                List.of(person("P01", "MALE"), person("P02", "MALE"), person("P03", "UNKNOWN")),
                List.of(edge("P01", "P03"), edge("P01", "P03"), edge("P02", "P03")),
                familyQuery("P03", "2", "ANCESTORS")
        ));
        assertError(result, ValidationErrorCode.DUPLICATE_EDGE);
    }

    @Test
    void directedCycleHasPriorityOverParentGenderError() {
        GraphValidationResult result = validator.validate(input(
                List.of(person("P01", "MALE"), person("P02", "MALE"), person("P03", "FEMALE")),
                List.of(edge("P01", "P03"), edge("P02", "P03"), edge("P03", "P01")),
                familyQuery("P03", "2", "ANCESTORS")
        ));
        assertError(result, ValidationErrorCode.DIRECTED_CYCLE);
        assertEquals(List.of("P01", "P03", "P01"), result.error().cycle());
    }

    @Test
    void validatesEveryDocumentedErrorCategory() {
        assertError(validator.validate(new ParsedInput(
                new ParsedFamilyGraph(2, List.of(person("P01", "MALE")), 0, List.of()),
                familyQuery("P01", "1", "BOTH"))), ValidationErrorCode.INVALID_COUNT);

        assertError(validate(List.of(person("P01", "MALE"), person("P01", "MALE")),
                List.of(), familyQuery("P01", "1", "BOTH")), ValidationErrorCode.DUPLICATE_ID);
        assertError(validate(List.of(person("P01", "OTHER")), List.of(),
                familyQuery("P01", "1", "BOTH")), ValidationErrorCode.INVALID_GENDER);
        assertError(validate(List.of(person("P01", "MALE")), List.of(edge("P01", "P99")),
                familyQuery("P01", "1", "BOTH")), ValidationErrorCode.UNKNOWN_ID);
        assertError(validate(List.of(person("P01", "MALE"), person("P02", "FEMALE")),
                List.of(edge("P01", "P02"), edge("P01", "P02")),
                familyQuery("P02", "2", "ANCESTORS")), ValidationErrorCode.DUPLICATE_EDGE);
        assertError(validate(List.of(person("P01", "MALE")), List.of(edge("P01", "P01")),
                familyQuery("P01", "1", "BOTH")), ValidationErrorCode.SELF_PARENT);
        assertError(validate(List.of(person("P01", "MALE")), List.of(),
                new ParsedQuery("UNKNOWN", List.of("P01"))), ValidationErrorCode.INVALID_QUERY_TYPE);
        assertError(validate(List.of(person("P01", "MALE")), List.of(),
                familyQuery("P01", "zero", "BOTH")), ValidationErrorCode.INVALID_GENERATION);
        assertError(validate(List.of(person("P01", "MALE")), List.of(),
                familyQuery("P01", "1", "SIDEWAYS")), ValidationErrorCode.INVALID_DIRECTION);
        assertError(validate(List.of(person("P01", "MALE")), List.of(),
                new ParsedQuery("CHECK_RELATIONSHIP", List.of("P01", "P01"))),
                ValidationErrorCode.SAME_PERSON_QUERY);
    }

    private GraphValidationResult validate(List<ParsedPerson> persons,
                                           List<ParsedParentChildEdge> edges,
                                           ParsedQuery query) {
        return validator.validate(input(persons, edges, query));
    }

    private ParsedInput input(List<ParsedPerson> persons,
                              List<ParsedParentChildEdge> edges,
                              ParsedQuery query) {
        return new ParsedInput(
                new ParsedFamilyGraph(persons.size(), persons, edges.size(), edges), query
        );
    }

    private ParsedPerson person(String id, String gender) {
        return new ParsedPerson(id, gender, "");
    }

    private ParsedParentChildEdge edge(String parent, String child) {
        return new ParsedParentChildEdge(parent, child);
    }

    private ParsedQuery familyQuery(String id, String generations, String direction) {
        return new ParsedQuery("FAMILY_TREE", List.of(id, generations, direction));
    }

    private void assertError(GraphValidationResult result, ValidationErrorCode code) {
        assertEquals(false, result.valid());
        assertEquals(code, result.error().code());
    }
}
