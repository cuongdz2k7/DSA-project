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
}
