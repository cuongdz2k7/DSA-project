package com.familygraph.familytree;

import com.familygraph.model.familytree.TopoResult;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopologicalSorterTest {
    private TopologicalSorter sorter;

    @BeforeEach
    void setUp() {
        sorter = new TopologicalSorter();
    }

    @Test
    @DisplayName("Đồ thị cơ bản: cha/mẹ -> con -> cháu")
    void sortsBasicFamilyTree() {
        List<Person> persons = List.of(
                new Person("P01", Gender.MALE, "Nam"),
                new Person("P02", Gender.FEMALE, "Hoa"),
                new Person("P03", Gender.MALE, "Cuong"),
                new Person("P04", Gender.FEMALE, "An")
        );
        List<ParentChildEdge> edges = List.of(
                new ParentChildEdge("P01", "P03"),
                new ParentChildEdge("P02", "P03"),
                new ParentChildEdge("P03", "P04")
        );

        TopoResult result = sorter.topologicalSort(new FamilyGraph(persons, edges));

        assertEquals(List.of("P01", "P02", "P03", "P04"), result.topoOrder());
    }

    @Test
    @DisplayName("Giữ thứ tự input khi nhiều đỉnh cùng bậc vào bằng 0")
    void keepsInputOrderForSameInDegree() {
        List<Person> persons = List.of(
                new Person("P03", Gender.MALE, "A"),
                new Person("P01", Gender.MALE, "B"),
                new Person("P02", Gender.FEMALE, "C")
        );

        TopoResult result = sorter.topologicalSort(new FamilyGraph(persons, List.of()));

        assertEquals(List.of("P03", "P01", "P02"), result.topoOrder());
    }

    @Test
    @DisplayName("Sắp xếp được nhiều gia đình không liên thông")
    void sortsDisconnectedFamilies() {
        List<Person> persons = List.of(
                new Person("P01", Gender.MALE, "A"),
                new Person("P02", Gender.FEMALE, "B"),
                new Person("P03", Gender.FEMALE, "C"),
                new Person("P04", Gender.FEMALE, "D")
        );
        List<ParentChildEdge> edges = List.of(
                new ParentChildEdge("P01", "P02"),
                new ParentChildEdge("P03", "P04")
        );

        TopoResult result = sorter.topologicalSort(new FamilyGraph(persons, edges));

        assertEquals(List.of("P01", "P03", "P02", "P04"), result.topoOrder());
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

    // test theo slide
    @Test 
    @DisplayName("Sắp xếp cây gia phả 4 thế hệ")
    void testSlideFamilyTreeTopoSorter(){
        List<Person>  persons = List.of(
            new Person("P01", Gender.MALE, "P01 Nam"),
            new Person("P02", Gender.FEMALE, "P02 Nữ"),
            new Person("P03", Gender.MALE, "P03 Nam"),
            new Person("P04", Gender.FEMALE, "P04 Nữ"),
            new Person("P05", Gender.MALE, "P05 Nam"),
            new Person("P06", Gender.FEMALE, "P06 Nữ"),
            new Person("P07", Gender.FEMALE, "P07 Nữ"),
            new Person("P08", Gender.UNKNOWN, "P08 Chưa rõ")
        );

        List<ParentChildEdge> edges = List.of(
            new ParentChildEdge("P01", "P05"),
            new ParentChildEdge("P02", "P05"),
            new ParentChildEdge("P03", "P06"),
            new ParentChildEdge("P04", "P06"),
            new ParentChildEdge("P05", "P07"),
            new ParentChildEdge("P06", "P07"),
            new ParentChildEdge("P05", "P08"), // Cạnh bắc cầu P05 -> P08
            new ParentChildEdge("P07", "P08")
        );
        FamilyGraph graph = new FamilyGraph(persons, edges);
        TopoResult result = sorter.topologicalSort(graph);

        List<String> expectedOrder = List.of(
            "P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08"
        );

        assertEquals(expectedOrder, result.topoOrder());
    }
}


