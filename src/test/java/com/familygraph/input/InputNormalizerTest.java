package com.familygraph.input;

import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedInput;
import com.familygraph.input.model.ParsedParentChildEdge;
import com.familygraph.input.model.ParsedPerson;
import com.familygraph.input.model.ParsedQuery;
import com.familygraph.input.model.ValidatedInput;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.validation.NormalizedInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class InputNormalizerTest {
    @Test
    void convertsValidatedObjectsAndPreservesInputOrder() {
        ValidatedInput input = new ValidatedInput(new ParsedInput(
                new ParsedFamilyGraph(3, List.of(
                        new ParsedPerson("P02", "FEMALE", "Hoa"),
                        new ParsedPerson("P01", "MALE", "Nam"),
                        new ParsedPerson("P03", "UNKNOWN", "")
                ), 2, List.of(
                        new ParsedParentChildEdge("P02", "P03"),
                        new ParsedParentChildEdge("P01", "P03")
                )),
                new ParsedQuery("FAMILY_TREE", List.of("P03", "2", "BOTH"))
        ));

        NormalizedInput result = new InputNormalizer().normalize(input);

        assertEquals(List.of("P02", "P01", "P03"),
                result.graph().persons().stream().map(person -> person.id()).toList());
        assertEquals(List.of(Gender.FEMALE, Gender.MALE, Gender.UNKNOWN),
                result.graph().persons().stream().map(person -> person.gender()).toList());
        assertEquals("P02", result.graph().edges().get(0).parentId());
        FamilyTreeQuery query = assertInstanceOf(FamilyTreeQuery.class, result.query());
        assertEquals(2, query.numberOfGenerations());
        assertEquals(Direction.BOTH, query.direction());
    }

    @Test
    void convertsRelationshipQueryToOfficialType() {
        ValidatedInput input = new ValidatedInput(new ParsedInput(
                new ParsedFamilyGraph(2, List.of(
                        new ParsedPerson("P01", "MALE", "A"),
                        new ParsedPerson("P02", "FEMALE", "B")
                ), 0, List.of()),
                new ParsedQuery("CHECK_RELATIONSHIP", List.of("P01", "P02"))
        ));

        NormalizedInput result = new InputNormalizer().normalize(input);

        CheckRelationshipQuery query = assertInstanceOf(CheckRelationshipQuery.class, result.query());
        assertEquals("P01", query.firstPersonId());
        assertEquals("P02", query.secondPersonId());
    }
}
