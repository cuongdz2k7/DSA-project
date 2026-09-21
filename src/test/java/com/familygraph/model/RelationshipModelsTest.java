package com.familygraph.model;

import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.relationship.RelationshipPath;
import com.familygraph.model.relationship.RelationshipResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipModelsTest {

    @Test
    void representsTwoPathsToACommonAncestor() {
        CheckRelationshipQuery query = new CheckRelationshipQuery("P05", "P06");
        RelationshipPath firstPath = new RelationshipPath(
                "P05",
                "P01",
                List.of("P05", "P03", "P01")
        );
        RelationshipPath secondPath = new RelationshipPath(
                "P06",
                "P01",
                List.of("P06", "P04", "P01")
        );

        RelationshipResult result = new RelationshipResult(
                query,
                List.of("P01"),
                List.of(firstPath, secondPath),
                List.of(
                        new Person("P01", Gender.MALE, "Nam"),
                        new Person("P03", Gender.MALE, "Binh"),
                        new Person("P04", Gender.FEMALE, "Lan"),
                        new Person("P05", Gender.MALE, "Minh"),
                        new Person("P06", Gender.FEMALE, "An")
                ),
                List.of(
                        new ParentChildEdge("P01", "P03"),
                        new ParentChildEdge("P01", "P04"),
                        new ParentChildEdge("P03", "P05"),
                        new ParentChildEdge("P04", "P06")
                )
        );

        assertTrue(result.related());
        assertEquals(2, firstPath.edgeCount());
    }

    @Test
    void representsNoDetectedRelationshipWithoutWarningsOrDepthFields() {
        RelationshipResult result = new RelationshipResult(
                new CheckRelationshipQuery("P05", "P09"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        assertFalse(result.related());
    }

    @Test
    void copiesEvidenceCollectionsWithoutRevalidatingAlgorithmOutput() {
        CheckRelationshipQuery query = new CheckRelationshipQuery("P05", "P06");
        RelationshipPath firstPath = new RelationshipPath("P05", "P01", List.of("P05", "P01"));
        RelationshipPath secondPath = new RelationshipPath("P06", "P01", List.of("P06", "P01"));
        List<Person> persons = new ArrayList<>(List.of(
                new Person("P01", Gender.UNKNOWN, ""),
                new Person("P05", Gender.UNKNOWN, ""),
                new Person("P06", Gender.UNKNOWN, "")
        ));
        List<ParentChildEdge> edges = new ArrayList<>(List.of(
                new ParentChildEdge("P01", "P05"),
                new ParentChildEdge("P01", "P06")
        ));

        RelationshipResult result = new RelationshipResult(
                query,
                List.of("P01"),
                List.of(secondPath, firstPath),
                persons,
                edges
        );
        persons.clear();
        edges.clear();

        assertEquals(3, result.evidencePersons().size());
        assertEquals(2, result.evidenceEdges().size());
        assertThrows(UnsupportedOperationException.class, () -> result.evidenceEdges().clear());
    }
}
