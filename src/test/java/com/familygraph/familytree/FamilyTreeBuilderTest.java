package com.familygraph.familytree;

import com.familygraph.model.familytree.FamilyNode;
import com.familygraph.model.familytree.OrderedFamilyGraph;
import com.familygraph.model.familytree.TopoResult;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FamilyTreeBuilderTest {
    private final FamilyTreeBuilder builder = new FamilyTreeBuilder();
    private final TopologicalSorter sorter = new TopologicalSorter();

    @Test
    void buildsBothDirectionsInFilteredTopoOrder() {
        FamilyGraph graph = basicFamilyGraph();
        FamilyTreeQuery query = new FamilyTreeQuery("P03", 2, Direction.BOTH);

        OrderedFamilyGraph result = builder.buildFamilyView(graph, query, sorter.topologicalSort(graph));

        assertEquals(List.of("P01", "P02", "P03", "P04"), result.topoOrder());
        assertEquals(result.topoOrder(), result.nodes().stream().map(node -> node.person().id()).toList());
        assertEquals(Map.of(
                "P01", List.of(1),
                "P02", List.of(1),
                "P03", List.of(0),
                "P04", List.of(-1)
        ), levelsById(result.nodes()));
        assertEquals(graph.edges(), result.edges());
    }

    @Test
    void limitsAncestorDepthAndAcceptsMissingParents() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        new Person("P01", Gender.MALE, "Ông"),
                        new Person("P02", Gender.FEMALE, "Mẹ"),
                        new Person("P03", Gender.MALE, "Con")
                ),
                List.of(
                        new ParentChildEdge("P01", "P02"),
                        new ParentChildEdge("P02", "P03")
                )
        );

        OrderedFamilyGraph result = builder.buildFamilyView(
                graph,
                new FamilyTreeQuery("P03", 2, Direction.ANCESTORS),
                sorter.topologicalSort(graph)
        );

        assertEquals(List.of("P02", "P03"), result.topoOrder());
        assertEquals(Map.of("P02", List.of(1), "P03", List.of(0)), levelsById(result.nodes()));
        assertEquals(List.of(new ParentChildEdge("P02", "P03")), result.edges());
    }

    @Test
    void buildsDescendantsAndReturnsOnlyTargetWhenOneGenerationRequested() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        new Person("P01", Gender.MALE, "Cha"),
                        new Person("P02", Gender.FEMALE, "Con"),
                        new Person("P03", Gender.MALE, "Cháu")
                ),
                List.of(
                        new ParentChildEdge("P01", "P02"),
                        new ParentChildEdge("P02", "P03")
                )
        );

        OrderedFamilyGraph descendants = builder.buildFamilyView(
                graph,
                new FamilyTreeQuery("P01", 2, Direction.DESCENDANTS),
                sorter.topologicalSort(graph)
        );
        OrderedFamilyGraph onlyTarget = builder.buildFamilyView(
                graph,
                new FamilyTreeQuery("P02", 1, Direction.BOTH),
                sorter.topologicalSort(graph)
        );

        assertEquals(List.of("P01", "P02"), descendants.topoOrder());
        assertEquals(Map.of("P01", List.of(0), "P02", List.of(-1)), levelsById(descendants.nodes()));
        assertEquals(List.of(new ParentChildEdge("P01", "P02")), descendants.edges());
        assertEquals(List.of("P02"), onlyTarget.topoOrder());
        assertEquals(Map.of("P02", List.of(0)), levelsById(onlyTarget.nodes()));
        assertEquals(List.of(), onlyTarget.edges());
    }

    @Test
    void keepsEveryDistinctRelationLevelForTheSamePerson() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        new Person("P10", Gender.MALE, "Nam"),
                        new Person("P11", Gender.MALE, "Bình"),
                        new Person("P12", Gender.FEMALE, "Lan"),
                        new Person("P13", Gender.MALE, "Minh")
                ),
                List.of(
                        new ParentChildEdge("P10", "P11"),
                        new ParentChildEdge("P11", "P12"),
                        new ParentChildEdge("P12", "P13"),
                        new ParentChildEdge("P10", "P13")
                )
        );

        OrderedFamilyGraph result = builder.buildFamilyView(
                graph,
                new FamilyTreeQuery("P13", 4, Direction.ANCESTORS),
                sorter.topologicalSort(graph)
        );

        assertEquals(List.of(1, 3), levelsById(result.nodes()).get("P10"));
        assertEquals(graph.edges(), result.edges());
    }

    private FamilyGraph basicFamilyGraph() {
        return new FamilyGraph(
                List.of(
                        new Person("P01", Gender.MALE, "Nam"),
                        new Person("P02", Gender.FEMALE, "Hoa"),
                        new Person("P03", Gender.MALE, "Cường"),
                        new Person("P04", Gender.FEMALE, "An")
                ),
                List.of(
                        new ParentChildEdge("P01", "P03"),
                        new ParentChildEdge("P02", "P03"),
                        new ParentChildEdge("P03", "P04")
                )
        );
    }

    private Map<String, List<Integer>> levelsById(List<FamilyNode> nodes) {
        return nodes.stream().collect(Collectors.toMap(
                node -> node.person().id(),
                FamilyNode::relationLevels
        ));
    }
    @Test
    @DisplayName("Test nâng cao: Gia phả 4 thế hệ, đa nhánh, con riêng và cụm không liên thông")
    void testComplexMultiGenerationalFamilyTree() {
        // 1. Danh sách 12 người trải qua 4 thế hệ
        List<Person> persons = List.of(
            new Person("P01", Gender.MALE, "Ông 1"),
            new Person("P02", Gender.FEMALE, "Bà 1"),
            new Person("P03", Gender.MALE, "Ông 2"),
            new Person("P04", Gender.FEMALE, "Bà Cụm Rời"),
            new Person("P05", Gender.MALE, "Bố 1"),
            new Person("P06", Gender.FEMALE, "Mẹ 1"),
            new Person("P07", Gender.FEMALE, "Cô / Con riêng P01"),
            new Person("P08", Gender.MALE, "Con trai"),
            new Person("P09", Gender.FEMALE, "Con gái"),
            new Person("P10", Gender.MALE, "Con Cụm Rời"),
            new Person("P11", Gender.MALE, "Cháu trai"),
            new Person("P12", Gender.FEMALE, "Cháu gái")
        );

        // 2. Danh sách quan hệ phức tạp
        List<ParentChildEdge> edges = List.of(
            new ParentChildEdge("P01", "P05"),
            new ParentChildEdge("P02", "P05"),
            new ParentChildEdge("P01", "P07"),
            new ParentChildEdge("P03", "P06"),
            new ParentChildEdge("P04", "P10"), // Nhánh không liên thông
            new ParentChildEdge("P05", "P08"),
            new ParentChildEdge("P06", "P08"),
            new ParentChildEdge("P05", "P09"),
            new ParentChildEdge("P07", "P09"), // Đan nhánh giữa P05 và P07
            new ParentChildEdge("P08", "P11"),
            new ParentChildEdge("P09", "P11"), // P11 chờ cả P08 và P09
            new ParentChildEdge("P08", "P12")
        );

        FamilyGraph graph = new FamilyGraph(persons, edges);
        TopoResult result = sorter.topologicalSort(graph);

        // 3. Kết quả mong đợi chính xác tuyệt đối theo thứ tự tie-breaking
        List<String> expectedOrder = List.of(
            "P01", "P02", "P03", "P04",
            "P07", "P05", "P06", "P10",
            "P09", "P08",
            "P11", "P12"
        );

        assertEquals(expectedOrder, result.topoOrder());
    }
}
