package com.familygraph.input;

import com.familygraph.model.graph.Gender;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.validation.ValidationErrorCode;
import com.familygraph.model.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawInputParserTest {
    private final RawInputParser parser = new RawInputParser();

    @Test
    void parsesFamilyTreeQueryAndPreservesInputOrder() {
        String rawInput = """
                PERSON_COUNT 4
                PERSON P01 MALE "Nam"
                PERSON P02 FEMALE "Hoa Nguyễn"
                PERSON P03 UNKNOWN
                PERSON P04 FEMALE An
                EDGE_COUNT 3
                EDGE P02 P03
                EDGE P01 P03
                EDGE P03 P04
                QUERY FAMILY_TREE P03 2 BOTH
                """;

        ValidationResult result = parser.parseAndValidate(rawInput);

        assertTrue(result.valid());
        assertEquals(List.of("P01", "P02", "P03", "P04"),
                result.data().graph().persons().stream().map(person -> person.id()).toList());
        assertEquals(List.of("Nam", "Hoa Nguyễn", "", "An"),
                result.data().graph().persons().stream().map(person -> person.name()).toList());
        assertEquals(List.of(Gender.MALE, Gender.FEMALE, Gender.UNKNOWN, Gender.FEMALE),
                result.data().graph().persons().stream().map(person -> person.gender()).toList());
        assertEquals("P02", result.data().graph().edges().get(0).parentId());
        assertEquals("P01", result.data().graph().edges().get(1).parentId());

        FamilyTreeQuery query = assertInstanceOf(FamilyTreeQuery.class, result.data().query());
        assertEquals("P03", query.targetId());
        assertEquals(2, query.numberOfGenerations());
        assertEquals(Direction.BOTH, query.direction());
    }

    @Test
    void parsesRelationshipQueryWithBlankLinesAndExtraWhitespace() {
        String rawInput = """

                  PERSON_COUNT    2
                PERSON   P01   MALE   "Nguyễn Văn A"

                PERSON P02 FEMALE "Trần Thị B"
                EDGE_COUNT 0

                QUERY   CHECK_RELATIONSHIP   P01   P02
                """;

        ValidationResult result = parser.parseAndValidate(rawInput);

        assertTrue(result.valid());
        assertEquals(List.of(), result.data().graph().edges());
        CheckRelationshipQuery query = assertInstanceOf(
                CheckRelationshipQuery.class,
                result.data().query()
        );
        assertEquals("P01", query.firstPersonId());
        assertEquals("P02", query.secondPersonId());
    }

    @Test
    void rejectsTwoKnownParentsWithTheSameGender() {
        String rawInput = """
                PERSON_COUNT 3
                PERSON P01 FEMALE "A"
                PERSON P02 FEMALE "B"
                PERSON P03 MALE "C"
                EDGE_COUNT 2
                EDGE P01 P03
                EDGE P02 P03
                QUERY FAMILY_TREE P03 2 ANCESTORS
                """;

        ValidationResult result = parser.parseAndValidate(rawInput);

        assertEquals(false, result.valid());
        assertEquals(ValidationErrorCode.SAME_GENDER_PARENTS, result.error().code());
        assertEquals("child=P03 parents=P01,P02 gender=FEMALE", result.error().detail());
    }

    @Test
    void acceptsUndirectedCycleWhenDirectedGraphIsAcyclic() {
        String rawInput = """
                PERSON_COUNT 3
                PERSON P01 MALE
                PERSON P02 FEMALE
                PERSON P03 UNKNOWN
                EDGE_COUNT 3
                EDGE P01 P02
                EDGE P01 P03
                EDGE P02 P03
                QUERY FAMILY_TREE P03 3 ANCESTORS
                """;

        assertTrue(parser.parseAndValidate(rawInput).valid());
    }

    @Test
    void returnsMalformedInputForNullEmptyOrInvalidStructure() {
        assertError(null, ValidationErrorCode.MALFORMED_INPUT);
        assertError("   \n\n", ValidationErrorCode.MALFORMED_INPUT);
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE "Missing quote
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.MALFORMED_INPUT);
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                EXTRA DATA
                """, ValidationErrorCode.MALFORMED_INPUT);
    }

    @Test
    void returnsInvalidCountForPersonOrEdgeMismatch() {
        assertError("""
                PERSON_COUNT 2
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.INVALID_COUNT);
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 1
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.INVALID_COUNT);
        assertError("""
                PERSON_COUNT 0
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.INVALID_COUNT);
    }

    @Test
    void returnsDuplicateIdBeforeInvalidGender() {
        String rawInput = """
                PERSON_COUNT 2
                PERSON P01 MALE
                PERSON P01 INVALID
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """;

        assertError(rawInput, ValidationErrorCode.DUPLICATE_ID, "P01");
    }

    @Test
    void returnsInvalidGender() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 OTHER
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.INVALID_GENDER, "OTHER");
    }

    @Test
    void returnsUnknownIdFromEdgesBeforeQuery() {
        String rawInput = """
                PERSON_COUNT 2
                PERSON P01 MALE
                PERSON P02 FEMALE
                EDGE_COUNT 1
                EDGE P99 P01
                QUERY FAMILY_TREE P88 1 BOTH
                """;

        assertError(rawInput, ValidationErrorCode.UNKNOWN_ID, "P99");
    }

    @Test
    void returnsUnknownIdFromQuery() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY CHECK_RELATIONSHIP P01 P02
                """, ValidationErrorCode.UNKNOWN_ID, "P02");
    }

    @Test
    void returnsDuplicateEdgeBeforeSelfParent() {
        String rawInput = """
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 2
                EDGE P01 P01
                EDGE P01 P01
                QUERY FAMILY_TREE P01 1 BOTH
                """;

        assertError(rawInput, ValidationErrorCode.DUPLICATE_EDGE, "P01 P01");
    }

    @Test
    void returnsSelfParent() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 1
                EDGE P01 P01
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.SELF_PARENT, "P01");
    }

    @Test
    void returnsFirstDirectedCycleAsClosedPath() {
        String rawInput = """
                PERSON_COUNT 4
                PERSON P01 MALE
                PERSON P02 FEMALE
                PERSON P03 MALE
                PERSON P04 FEMALE
                EDGE_COUNT 4
                EDGE P01 P02
                EDGE P02 P03
                EDGE P03 P01
                EDGE P03 P04
                QUERY FAMILY_TREE P04 2 ANCESTORS
                """;

        ValidationResult result = parser.parseAndValidate(rawInput);

        assertEquals(ValidationErrorCode.DIRECTED_CYCLE, result.error().code());
        assertEquals(List.of("P01", "P02", "P03", "P01"), result.error().cycle());
    }

    @Test
    void returnsInvalidQueryType() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY UNKNOWN P01
                """, ValidationErrorCode.INVALID_QUERY_TYPE, "UNKNOWN");
    }

    @Test
    void returnsInvalidGenerationForNonNumberZeroOrNegative() {
        assertError(familyTreeInputWithGeneration("abc"), ValidationErrorCode.INVALID_GENERATION, "abc");
        assertError(familyTreeInputWithGeneration("0"), ValidationErrorCode.INVALID_GENERATION, "0");
        assertError(familyTreeInputWithGeneration("-2"), ValidationErrorCode.INVALID_GENERATION, "-2");
    }

    @Test
    void returnsInvalidDirection() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 SIDEWAYS
                """, ValidationErrorCode.INVALID_DIRECTION, "SIDEWAYS");
    }

    @Test
    void returnsSamePersonQuery() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY CHECK_RELATIONSHIP P01 P01
                """, ValidationErrorCode.SAME_PERSON_QUERY, "P01");
    }

    @Test
    void keywordsAndEnumsAreCaseSensitive() {
        assertError("""
                PERSON_COUNT 1
                PERSON P01 male
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.INVALID_GENDER, "male");
        assertError("""
                person_count 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """, ValidationErrorCode.MALFORMED_INPUT);
    }

    private String familyTreeInputWithGeneration(String generation) {
        return """
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 %s BOTH
                """.formatted(generation);
    }

    private void assertError(String rawInput, ValidationErrorCode expectedCode) {
        ValidationResult result = parser.parseAndValidate(rawInput);
        assertEquals(false, result.valid());
        assertEquals(expectedCode, result.error().code());
        if (expectedCode != ValidationErrorCode.DIRECTED_CYCLE) {
            assertEquals(List.of(), result.error().cycle());
        }
    }

    private void assertError(
            String rawInput,
            ValidationErrorCode expectedCode,
            String expectedDetail
    ) {
        ValidationResult result = parser.parseAndValidate(rawInput);
        assertEquals(false, result.valid());
        assertEquals(expectedCode, result.error().code());
        assertEquals(expectedDetail, result.error().detail());
        assertEquals(List.of(), result.error().cycle());
    }
}
