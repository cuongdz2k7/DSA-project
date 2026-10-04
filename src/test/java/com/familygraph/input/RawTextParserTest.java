package com.familygraph.input;

import com.familygraph.input.model.ParseResult;
import com.familygraph.model.validation.ValidationErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawTextParserTest {
    private final RawTextParser parser = new RawTextParser();

    @Test
    void parsesTokensWithoutApplyingDomainValidation() {
        ParseResult result = parser.parse("""
                PERSON_COUNT 1
                PERSON P01 OTHER "Nguyễn Văn A"
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 abc SIDEWAYS
                """);

        assertTrue(result.successful());
        assertEquals("OTHER", result.data().graph().persons().get(0).genderToken());
        assertEquals("Nguyễn Văn A", result.data().graph().persons().get(0).name());
        assertEquals(List.of("P01", "abc", "SIDEWAYS"), result.data().query().arguments());
    }

    @Test
    void acceptsBlankLinesWhitespaceAndAllSupportedNameForms() {
        ParseResult result = parser.parse("""

                  PERSON_COUNT 3
                PERSON P01 MALE
                PERSON P02 FEMALE Hoa
                PERSON P03 UNKNOWN "Nguyễn Văn An"

                EDGE_COUNT 0
                QUERY CHECK_RELATIONSHIP P01 P02
                """);

        assertTrue(result.successful());
        assertEquals(List.of("", "Hoa", "Nguyễn Văn An"),
                result.data().graph().persons().stream().map(person -> person.name()).toList());
    }

    @Test
    void returnsMalformedForMissingMarkerBrokenQuoteOrTrailingLine() {
        assertMalformed("PERSON_COUNT 1\nPERSON P01 MALE\nQUERY FAMILY_TREE P01 1 BOTH");
        assertMalformed("""
                PERSON_COUNT 1
                PERSON P01 MALE "Missing quote
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """);
        assertMalformed("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                EXTRA
                """);
    }

    @Test
    void parsedCollectionsAreImmutableCopies() {
        ParseResult result = parser.parse("""
                PERSON_COUNT 1
                PERSON P01 MALE
                EDGE_COUNT 0
                QUERY FAMILY_TREE P01 1 BOTH
                """);
        assertEquals(1, result.data().graph().persons().size());
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> result.data().graph().persons().clear()
        );
    }

    private void assertMalformed(String input) {
        ParseResult result = parser.parse(input);
        assertFalse(result.successful());
        assertEquals(ValidationErrorCode.MALFORMED_INPUT, result.error().code());
    }
}
