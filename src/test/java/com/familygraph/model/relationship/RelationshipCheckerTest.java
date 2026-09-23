package com.familygraph.model.relationship;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelationshipCheckerTest {

    @Test
    void findsCommonAncestorWithinThreeGenerations() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        person("P01"),
                        person("P03"),
                        new Person("P04", Gender.MALE, ""),
                        new Person("P05", Gender.MALE, ""),
                        new Person("P06", Gender.FEMALE, "")
                ),
                List.of(
                        new ParentChildEdge("P01", "P03"),
                        new ParentChildEdge("P01", "P04"),
                        new ParentChildEdge("P03", "P05"),
                        new ParentChildEdge("P04", "P06")
                )
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph,
                new CheckRelationshipQuery("P05", "P06")
        );

        assertTrue(result.related());
        assertEquals(List.of("P01"), result.commonAncestorIds());
        assertEquals(
                List.of("P05", "P03", "P01"),
                result.paths().get(0).nodeIds()
        );
        assertEquals(
                List.of("P06", "P04", "P01"),
                result.paths().get(1).nodeIds()
        );
        assertEquals(graph.persons(), result.evidencePersons());
        assertEquals(graph.edges(), result.evidenceEdges());
    }

    @Test
    void includesTheSourceToDetectDirectAncestry() {
        FamilyGraph graph = new FamilyGraph(
                List.of(new Person("P01", Gender.MALE, ""), new Person("P02", Gender.FEMALE, "")),
                List.of(new ParentChildEdge("P01", "P02"))
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph,
                new CheckRelationshipQuery("P01", "P02")
        );

        assertTrue(result.related());
        assertEquals(List.of("P01"), result.commonAncestorIds());
        assertEquals(List.of("P01"), result.paths().get(0).nodeIds());
        assertEquals(List.of("P02", "P01"), result.paths().get(1).nodeIds());
    }

    @Test
    void doesNotSearchPastThreeGenerations() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        person("P01"),
                        person("P02"),
                        person("P03"),
                        new Person("P04", Gender.MALE, ""),
                        new Person("P06", Gender.FEMALE, ""),
                        person("P07"),
                        new Person("P08", Gender.FEMALE, "")
                ),
                List.of(
                        new ParentChildEdge("P01", "P02"),
                        new ParentChildEdge("P02", "P03"),
                        new ParentChildEdge("P03", "P04"),
                        new ParentChildEdge("P01", "P06"),
                        new ParentChildEdge("P06", "P07"),
                        new ParentChildEdge("P07", "P08")
                )
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph,
                new CheckRelationshipQuery("P04", "P08")
        );

        assertFalse(result.related());
        assertTrue(result.commonAncestorIds().isEmpty());
        assertTrue(result.paths().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"MALE, MALE", "FEMALE, FEMALE"})
    void returnsUnrelatedForSameKnownGenderEvenWithCommonAncestor(Gender first, Gender second) {
        RelationshipResult result = RelationshipChecker.checkRelationship(
                graphWithSiblings(first, second),
                new CheckRelationshipQuery("P01", "P02")
        );
        assertFalse(result.related());
        assertTrue(result.commonAncestorIds().isEmpty());
        assertTrue(result.paths().isEmpty());
        assertTrue(result.evidencePersons().isEmpty());
        assertTrue(result.evidenceEdges().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "UNKNOWN, MALE", "UNKNOWN, FEMALE", "MALE, UNKNOWN",
            "FEMALE, UNKNOWN", "UNKNOWN, UNKNOWN"
    })
    void rejectsUnknownGenderInEitherPositionIncludingBothUnknown(Gender first, Gender second) {
        assertThrows(GenderInvalid.class, () -> RelationshipChecker.checkRelationship(
                graphWithSiblings(first, second),
                new CheckRelationshipQuery("P01", "P02")
        ));
    }

    @ParameterizedTest
    @CsvSource({"MALE, FEMALE", "FEMALE, MALE"})
    void findsRelationshipForOppositeGendersWithUnknownGenderAncestor(Gender first, Gender second) {
        FamilyGraph graph = graphWithSiblings(first, second);
        CheckRelationshipQuery query = new CheckRelationshipQuery("P01", "P02");

        RelationshipResult result = RelationshipChecker.checkRelationship(graph, query);

        assertEquals(query, result.query());
        assertTrue(result.related());
        assertEquals(List.of("P00"), result.commonAncestorIds());
        assertEquals(List.of(
                new RelationshipPath("P01", "P00", List.of("P01", "P00")),
                new RelationshipPath("P02", "P00", List.of("P02", "P00"))
        ), result.paths());
        assertEquals(graph.persons(), result.evidencePersons());
        assertEquals(graph.edges(), result.evidenceEdges());
    }

    @Test
    void includesAllCommonAncestorsAndExcludesUnrelatedEvidence() {
        Person father = new Person("P10", Gender.MALE, "");
        Person mother = new Person("P00", Gender.FEMALE, "");
        Person first = new Person("P01", Gender.MALE, "");
        Person second = new Person("P02", Gender.FEMALE, "");
        List<ParentChildEdge> familyEdges = List.of(
                new ParentChildEdge("P10", "P01"), new ParentChildEdge("P10", "P02"),
                new ParentChildEdge("P00", "P01"), new ParentChildEdge("P00", "P02")
        );
        List<ParentChildEdge> edges = new ArrayList<>(familyEdges);
        edges.add(new ParentChildEdge("P10", "P99"));
        FamilyGraph graph = new FamilyGraph(
                List.of(father, mother, first, second, person("P99")), edges
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph, new CheckRelationshipQuery("P01", "P02")
        );

        assertTrue(result.related());
        assertEquals(List.of("P00", "P10"), result.commonAncestorIds());
        assertEquals(List.of(
                new RelationshipPath("P01", "P00", List.of("P01", "P00")),
                new RelationshipPath("P02", "P00", List.of("P02", "P00")),
                new RelationshipPath("P01", "P10", List.of("P01", "P10")),
                new RelationshipPath("P02", "P10", List.of("P02", "P10"))
        ), result.paths());
        assertEquals(List.of(father, mother, first, second), result.evidencePersons());
        assertEquals(familyEdges, result.evidenceEdges());
    }

    @Test
    void returnsUnrelatedForOppositeGendersWithoutCommonAncestor() {
        RelationshipResult result = RelationshipChecker.checkRelationship(
                new FamilyGraph(List.of(
                        new Person("P01", Gender.MALE, ""),
                        new Person("P02", Gender.FEMALE, "")
                ), List.of()),
                new CheckRelationshipQuery("P01", "P02")
        );
        assertFalse(result.related());
        assertTrue(result.commonAncestorIds().isEmpty());
        assertTrue(result.paths().isEmpty());
        assertTrue(result.evidencePersons().isEmpty());
        assertTrue(result.evidenceEdges().isEmpty());
    }

    private static FamilyGraph graphWithSiblings(Gender first, Gender second) {
        return new FamilyGraph(
                List.of(person("P00"), new Person("P01", first, ""), new Person("P02", second, "")),
                List.of(new ParentChildEdge("P00", "P01"), new ParentChildEdge("P00", "P02"))
        );
    }

    private static Person person(String id) {
        return new Person(id, Gender.UNKNOWN, "");
    }
}

