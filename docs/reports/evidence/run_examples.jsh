// Evidence caller only. Does not implement or replace the production algorithms.
// Run from the Maven project root with target/classes on the JShell classpath.
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;
import com.familygraph.input.*;
import com.familygraph.model.query.*;
import com.familygraph.familytree.*;
import com.familygraph.relationship.*;

String json(Object value) throws Exception {
    if (value == null) return "null";
    if (value instanceof String || value instanceof Enum<?>) {
        String s = value.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return "\"" + s + "\"";
    }
    if (value instanceof Boolean || value instanceof Number) return value.toString();
    if (value instanceof Collection<?> items) {
        List<String> parts = new ArrayList<>();
        for (Object item : items) parts.add(json(item));
        return "[" + String.join(",", parts) + "]";
    }
    if (value.getClass().isRecord()) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (RecordComponent part : value.getClass().getRecordComponents()) {
            fields.put(part.getName(), part.getAccessor().invoke(value));
        }
        return json(fields);
    }
    if (value instanceof Map<?, ?> fields) {
        List<String> parts = new ArrayList<>();
        for (var entry : fields.entrySet()) parts.add(json(entry.getKey().toString()) + ":" + json(entry.getValue()));
        return "{" + String.join(",", parts) + "}";
    }
    throw new IllegalArgumentException("Unsupported evidence type: " + value.getClass());
}

void observe(Path inputFile, Path outputDirectory) throws Exception {
    String raw = Files.readString(inputFile, StandardCharsets.UTF_8);
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("inputFile", inputFile.getFileName().toString());
    var parsed = new RawTextParser().parse(raw);
    snapshot.put("parseResult", parsed);
    if (parsed.successful()) {
        var checked = new ParsedInputValidator().validate(parsed.data());
        snapshot.put("graphValidationResult", checked);
    }
    var facade = new RawInputParser().parseAndValidate(raw);
    snapshot.put("validationResult", facade);
    if (facade.valid()) {
        var graph = facade.data().graph();
        var query = facade.data().query();
        if (query instanceof FamilyTreeQuery familyQuery) {
            var topo = new TopologicalSorter().topologicalSort(graph);
            var view = new FamilyTreeBuilder().buildFamilyView(graph, familyQuery, topo);
            var result = new FamilyTreePipeline().createFamilyTree(graph, familyQuery);
            snapshot.put("topoResult", topo);
            snapshot.put("orderedFamilyGraph", view);
            snapshot.put("familyTreeResult", result);
            Map<String, Object> codes = new LinkedHashMap<>();
            Map<String, Object> offsets = new LinkedHashMap<>();
            for (var person : result.persons()) {
                codes.put(person.person().id(), person.relations().stream().map(r -> r.code()).toList());
                offsets.put(person.person().id(), person.relations().stream().map(r -> r.generationOffset()).toList());
            }
            snapshot.put("relationCodesById", codes);
            snapshot.put("generationOffsetsById", offsets);
        } else if (query instanceof CheckRelationshipQuery relationshipQuery) {
            var result = RelationshipChecker.checkRelationship(graph, relationshipQuery);
            snapshot.put("relationshipResult", result);
            snapshot.put("related", result.related());
        } else {
            throw new IllegalStateException("Unknown query type");
        }
    }
    String stem = inputFile.getFileName().toString().replaceFirst("\\.txt$", "");
    Files.writeString(outputDirectory.resolve(stem + ".json"), json(snapshot) + "\n", StandardCharsets.UTF_8);
    System.out.println("OBSERVED " + stem + " valid=" + facade.valid());
}

Path examples = Path.of("docs/reports/examples");
Path outputDirectory = Path.of("docs/reports/evidence/actual");
Files.createDirectories(outputDirectory);
try (var files = Files.list(examples)) {
    for (Path file : files.filter(p -> p.toString().endsWith(".txt")).sorted().toList()) {
        observe(file, outputDirectory);
    }
}
/exit
