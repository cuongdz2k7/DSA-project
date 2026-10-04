package com.familygraph.input;

import com.familygraph.input.model.ParseResult;
import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedInput;
import com.familygraph.input.model.ParsedParentChildEdge;
import com.familygraph.input.model.ParsedPerson;
import com.familygraph.input.model.ParsedQuery;
import com.familygraph.model.validation.ValidationError;
import com.familygraph.model.validation.ValidationErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts raw text into syntax-level objects without applying domain rules. */
public final class RawTextParser {
    private static final Pattern PERSON_PATTERN = Pattern.compile(
            "^PERSON\\s+(\\S+)\\s+(\\S+)(?:\\s+(?:\"([^\"]*)\"|(\\S+)))?$"
    );

    public ParseResult parse(String rawInput) {
        try {
            return ParseResult.success(parseDocument(rawInput));
        } catch (MalformedInputException exception) {
            return ParseResult.failure(new ValidationError(
                    ValidationErrorCode.MALFORMED_INPUT,
                    exception.getMessage(),
                    List.of()
            ));
        }
    }

    private ParsedInput parseDocument(String rawInput) {
        if (rawInput == null) {
            throw malformed("rawInput is null");
        }

        List<String> lines = rawInput.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (lines.isEmpty()) {
            throw malformed("rawInput is empty");
        }

        int declaredPersonCount = parseCountLine(lines.get(0), "PERSON_COUNT", 1);
        int edgeCountIndex = findMarker(lines, "EDGE_COUNT", 1);
        if (edgeCountIndex < 0) {
            throw malformed("missing EDGE_COUNT");
        }

        List<ParsedPerson> persons = new ArrayList<>();
        for (int index = 1; index < edgeCountIndex; index++) {
            persons.add(parsePersonLine(lines.get(index), index + 1));
        }

        int declaredEdgeCount = parseCountLine(
                lines.get(edgeCountIndex), "EDGE_COUNT", edgeCountIndex + 1
        );
        int queryIndex = findMarker(lines, "QUERY", edgeCountIndex + 1);
        if (queryIndex < 0) {
            throw malformed("missing QUERY");
        }
        if (queryIndex != lines.size() - 1) {
            throw malformed("unexpected content after QUERY");
        }

        List<ParsedParentChildEdge> edges = new ArrayList<>();
        for (int index = edgeCountIndex + 1; index < queryIndex; index++) {
            edges.add(parseEdgeLine(lines.get(index), index + 1));
        }

        ParsedFamilyGraph graph = new ParsedFamilyGraph(
                declaredPersonCount, persons, declaredEdgeCount, edges
        );
        return new ParsedInput(graph, parseQueryLine(lines.get(queryIndex), queryIndex + 1));
    }

    private int parseCountLine(String line, String expectedKeyword, int lineNumber) {
        String[] tokens = splitTokens(line);
        if (tokens.length != 2 || !tokens[0].equals(expectedKeyword)) {
            throw malformed("line " + lineNumber + ": expected " + expectedKeyword);
        }
        try {
            return Integer.parseInt(tokens[1]);
        } catch (NumberFormatException exception) {
            throw malformed("line " + lineNumber + ": invalid count " + tokens[1]);
        }
    }

    private ParsedPerson parsePersonLine(String line, int lineNumber) {
        Matcher matcher = PERSON_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw malformed("line " + lineNumber + ": invalid PERSON record");
        }
        String unquotedName = matcher.group(4);
        if (unquotedName != null && unquotedName.contains("\"")) {
            throw malformed("line " + lineNumber + ": invalid PERSON name");
        }
        String name = matcher.group(3) != null
                ? matcher.group(3)
                : unquotedName == null ? "" : unquotedName;
        return new ParsedPerson(matcher.group(1), matcher.group(2), name);
    }

    private ParsedParentChildEdge parseEdgeLine(String line, int lineNumber) {
        String[] tokens = splitTokens(line);
        if (tokens.length != 3 || !tokens[0].equals("EDGE")) {
            throw malformed("line " + lineNumber + ": invalid EDGE record");
        }
        return new ParsedParentChildEdge(tokens[1], tokens[2]);
    }

    private ParsedQuery parseQueryLine(String line, int lineNumber) {
        String[] tokens = splitTokens(line);
        if (tokens.length < 2 || !tokens[0].equals("QUERY")) {
            throw malformed("line " + lineNumber + ": invalid QUERY record");
        }
        String type = tokens[1];
        if (type.equals("FAMILY_TREE") && tokens.length != 5) {
            throw malformed("line " + lineNumber + ": FAMILY_TREE requires 3 parameters");
        }
        if (type.equals("CHECK_RELATIONSHIP") && tokens.length != 4) {
            throw malformed("line " + lineNumber + ": CHECK_RELATIONSHIP requires 2 parameters");
        }
        return new ParsedQuery(type, List.of(tokens).subList(2, tokens.length));
    }

    private int findMarker(List<String> lines, String keyword, int startIndex) {
        for (int index = startIndex; index < lines.size(); index++) {
            String[] tokens = splitTokens(lines.get(index));
            if (tokens.length > 0 && tokens[0].equals(keyword)) {
                return index;
            }
        }
        return -1;
    }

    private String[] splitTokens(String line) {
        return line.trim().split("\\s+");
    }

    private MalformedInputException malformed(String detail) {
        return new MalformedInputException(detail);
    }

    private static final class MalformedInputException extends RuntimeException {
        private MalformedInputException(String message) {
            super(message);
        }
    }
}
