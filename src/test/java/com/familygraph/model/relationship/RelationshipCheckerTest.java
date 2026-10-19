package com.familygraph.model.relationship;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.relationship.RelationshipChecker;

/**
 * Test cho {@link RelationshipChecker#checkRelationship}.
 *
 * <p>Quy ước chung:
 * <ul>
 *   <li>{@code new ParentChildEdge("A", "B")} nghĩa là A là cha/mẹ của B (chiều parent → child).</li>
 *   <li>{@code person(id)} tạo người giới tính MALE. Muốn tạo nữ thì dùng
 *       {@code new Person(id, Gender.FEMALE, "")}.</li>
 *   <li>Giới hạn 3 đời = tối đa 2 cạnh từ người nguồn lên tổ tiên
 *       ({@code MAX_GENERATIONS - 1}).</li>
 *   <li>BFS không dùng visited: mọi đường tới cùng một tổ tiên đều được giữ lại
 *       (xem test [7], [8], [9]).</li>
 *   <li>Thứ tự kết quả: tổ tiên chung sort theo chuỗi ID; path nhóm theo từng tổ tiên,
 *       người thứ nhất đứng trước người thứ hai; bằng chứng giữ thứ tự gốc của graph.</li>
 * </ul>
 *
 * <p>Bản đồ test:
 * <pre>
 *   [1]  anh chị em họ, trong 3 đời            đường chính
 *   [2]  cha và con                            path hạt giống (trực hệ)
 *   [3]  tổ tiên cách 3 cạnh                   biên giới hạn 3 đời
 *   [4]  cùng giới                             thoát sớm theo giới tính
 *   [5]  khác giới                             đường chính, cả hai thứ tự giới tính
 *   [6]  nhiều tổ tiên chung + người thừa      sort + lọc bằng chứng
 *   [7]  hai đường tới cùng tổ tiên            không dùng visited
 *   [8]  đường ngắn và đường dài tới 1 tổ tiên không dùng visited, khác độ dài
 *   [9]  đường dài vượt giới hạn               cắt đường vòng quá 3 đời
 *   [10] khác giới, không có cạnh nào          giao tập rỗng
 *   [11] WORST CASE                            6 tổ tiên chung, 12 path
 * </pre>
 *
 * <p>Chưa có test cho:
 * <ul>
 *   <li>Giới tính {@code UNKNOWN} (hai người cùng UNKNOWN hiện bị coi là cùng giới).</li>
 *   <li>Hai người đều có tổ tiên nhưng hai tập tổ tiên không giao nhau.</li>
 *   <li>ID không tồn tại ({@code findPerson} đang ném NoSuchElementException).</li>
 * </ul>
 */
class RelationshipCheckerTest {

    /**
     * [1] Đường chính: anh chị em họ có chung ông, nằm đúng trong 3 đời.
     *
     * <pre>
     *        P01
     *       /   \
     *     P03   P04
     *      |     |
     *     P05   P06      query (P05, P06)
     * </pre>
     *
     * Kỳ vọng: related, tổ tiên chung [P01], path P05→P03→P01 rồi P06→P04→P01.
     * Phủ: BFS chạm đúng mức 2 cạnh (ông bà), giao tập, thứ tự path.
     * Lưu ý: không có người thừa nên bằng chứng = toàn bộ graph. Bước lọc bằng chứng
     * chỉ được kiểm tra thật sự ở test [6].
     */
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

    /**
     * [2] Quan hệ trực hệ: P01 là cha của P02, query (P01, P02).
     *
     * Kỳ vọng: tổ tiên chung [P01]; path của P01 là [P01] (0 cạnh), path của P02 là [P02, P01].
     * Phủ: path hạt giống. BFS bắt đầu bằng [sourceId] nên chính người nguồn cũng được
     * coi là tổ tiên của mình. Nếu bỏ path này thì quan hệ cha con sẽ không được phát hiện.
     * Lưu ý: test chưa assert evidencePersons và evidenceEdges.
     */
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

    /**
     * [3] Biên giới hạn 3 đời: tổ tiên chung nằm ở đời thứ 4 nên không được tìm thấy.
     *
     * <pre>
     *   P01 → P02 → P03 → P04       (P04 cách P01 3 cạnh)
     *   P01 → P06 → P07 → P08       (P08 cách P01 3 cạnh)    query (P04, P08)
     * </pre>
     *
     * Kỳ vọng: không related, commonAncestorIds và paths rỗng.
     * Phủ: điều kiện dừng {@code edgeCount() >= maxEdges}. P04 chỉ tới được P03 và P02
     * (tối đa 2 cạnh), P01 không bao giờ vào hàng đợi.
     * Đi cặp với test [1]: cách 2 cạnh thì thấy, cách 3 cạnh thì không.
     */
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

    /**
     * [4] Cùng giới thì không liên quan, dù có tổ tiên chung (hai anh em cùng cha P00).
     *
     * Kỳ vọng: mọi danh sách đều rỗng, kể cả bằng chứng.
     * Phủ: nhánh thoát sớm theo giới tính, BFS không chạy. Đồ thị có tổ tiên chung thật,
     * nên test chứng minh chính luật giới tính chặn kết quả.
     * Lưu ý: tên có chữ "Known" nhưng chỉ có hai bộ MALE/MALE và FEMALE/FEMALE.
     * Code so sánh bằng {@code ==} nên hai người cùng UNKNOWN cũng bị coi là cùng giới,
     * hành vi này chưa có test và chưa được nhóm thống nhất.
     */
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

    /**
     * [5] Khác giới thì liên quan khi có tổ tiên chung (hai anh chị em cùng cha P00).
     *
     * Kỳ vọng: related, tổ tiên chung [P00], hai RelationshipPath đúng sourceId, ancestorId
     * và nodeIds, bằng chứng = toàn bộ graph, và result.query() giống query đã gửi.
     * Phủ: đường chính theo cả hai thứ tự giới tính (MALE, FEMALE) và (FEMALE, MALE),
     * để chắc chắn kết quả không phụ thuộc ai đứng trước.
     * Đi cặp với test [4]: cùng một đồ thị, khác nhau ở giới tính nên kết quả đảo ngược.
     */
    @ParameterizedTest
    @CsvSource({"MALE, FEMALE", "FEMALE, MALE"})
    void findsRelationshipForOppositeGendersWithCommonAncestor(Gender first, Gender second) {
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

    /**
     * [6] Anh chị em ruột có hai tổ tiên chung, kèm một người thừa P99 không liên quan.
     *
     * <pre>
     *   P10 (cha) ─┬─► P01
     *              ├─► P02        query (P01, P02)
     *   P00 (mẹ) ──┴─► P01, P02
     *   P10 ─► P99                  (P99 là con khác của cha, phải bị loại khỏi bằng chứng)
     * </pre>
     *
     * Kỳ vọng:
     * <ul>
     *   <li>commonAncestorIds = [P00, P10]: sort theo chuỗi ID, không theo thứ tự trong graph
     *       (trong graph cha P10 đứng trước mẹ P00).</li>
     *   <li>paths nhóm theo tổ tiên: P00 trước, trong mỗi tổ tiên thì P01 trước P02.</li>
     *   <li>evidencePersons = [P10, P00, P01, P02] và evidenceEdges chỉ gồm 4 cạnh gia đình,
     *       không có P99 và cạnh P10→P99.</li>
     * </ul>
     * Phủ: đây là test duy nhất kiểm tra bước lọc bằng chứng thật sự (lọc từ graph gốc,
     * giữ thứ tự gốc).
     */
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

    /**
     * [7] Mỗi người có hai đường lên cùng một tổ tiên ở đúng mức 3 đời (dạng hình thoi).
     *
     * <pre>
     *            P00
     *        /  /   \  \
     *     P10 P11   P20 P21
     *       \ /       \ /
     *       P01       P02        query (P01, P02)
     * </pre>
     *
     * Kỳ vọng: tổ tiên chung [P00] nhưng có 4 path: P01 qua P10, P01 qua P11, P02 qua P20,
     * P02 qua P21. Bằng chứng = toàn bộ graph.
     * Phủ: đây là test bảo vệ thiết kế "BFS không dùng visited". Nếu thêm visited thì
     * đường thứ hai tới P00 bị chặn và test này sẽ fail.
     */
    @Test
    void includesAllPathsFromBothPeopleToTheSameCommonAncestor() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        person("P00"),
                        person("P10"),
                        new Person("P11", Gender.FEMALE, ""),
                        person("P20"),
                        new Person("P21", Gender.FEMALE, ""),
                        person("P01"),
                        new Person("P02", Gender.FEMALE, "")
                ),
                List.of(
                        new ParentChildEdge("P00", "P10"),
                        new ParentChildEdge("P00", "P11"),
                        new ParentChildEdge("P00", "P20"),
                        new ParentChildEdge("P00", "P21"),
                        new ParentChildEdge("P10", "P01"),
                        new ParentChildEdge("P11", "P01"),
                        new ParentChildEdge("P20", "P02"),
                        new ParentChildEdge("P21", "P02")
                )
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph, new CheckRelationshipQuery("P01", "P02")
        );

        assertTrue(result.related());
        assertEquals(List.of("P00"), result.commonAncestorIds());
        assertEquals(List.of(
                new RelationshipPath("P01", "P00", List.of("P01", "P10", "P00")),
                new RelationshipPath("P01", "P00", List.of("P01", "P11", "P00")),
                new RelationshipPath("P02", "P00", List.of("P02", "P20", "P00")),
                new RelationshipPath("P02", "P00", List.of("P02", "P21", "P00"))
        ), result.paths());
        assertEquals(graph.persons(), result.evidencePersons());
        assertEquals(graph.edges(), result.evidenceEdges());
    }

    /**
     * [8] P01 tới P00 bằng hai đường có độ dài khác nhau, cả hai đều trong 3 đời.
     *
     * <pre>
     *   P00 ─► P01                 đường ngắn: P01 → P00 (1 cạnh)
     *   P00 ─► P10 ─► P01          đường dài:  P01 → P10 → P00 (2 cạnh)
     *   P00 ─► P02                 query (P01, P02)
     * </pre>
     *
     * Kỳ vọng: path của P01 là [P01, P00] rồi [P01, P10, P00]; path của P02 là [P02, P00].
     * Phủ: không dùng visited, kể cả khi đường tới một node có độ dài khác nhau.
     * BFS có visited sẽ chỉ giữ đường ngắn và test này fail. Thứ tự "ngắn trước, dài sau"
     * là do hàng đợi FIFO nên test cũng khóa luôn thứ tự này.
     */
    @Test
    void includesLongerPathAlongsideShortestPathWithinThreeGenerations() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        person("P00"),
                        new Person("P10", Gender.FEMALE, ""),
                        person("P01"),
                        new Person("P02", Gender.FEMALE, "")
                ),
                List.of(
                        new ParentChildEdge("P00", "P10"),
                        new ParentChildEdge("P00", "P01"),
                        new ParentChildEdge("P10", "P01"),
                        new ParentChildEdge("P00", "P02")
                )
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph, new CheckRelationshipQuery("P01", "P02")
        );

        assertTrue(result.related());
        assertEquals(List.of("P00"), result.commonAncestorIds());
        assertEquals(List.of(
                new RelationshipPath("P01", "P00", List.of("P01", "P00")),
                new RelationshipPath("P01", "P00", List.of("P01", "P10", "P00")),
                new RelationshipPath("P02", "P00", List.of("P02", "P00"))
        ), result.paths());
        assertEquals(graph.persons(), result.evidencePersons());
        assertEquals(graph.edges(), result.evidenceEdges());
    }

    /**
     * [9] P01 có đường trực tiếp tới P00, nhưng còn một đường vòng dài 3 cạnh bị loại.
     *
     * <pre>
     *   P00 ─► P01, P00 ─► P02          đường trực tiếp (1 cạnh)
     *   P00 ─► P10 ─► P11 ─► P01        đường vòng P01 → P11 → P10 → P00 (3 cạnh, vượt giới hạn)
     * </pre>
     *
     * Kỳ vọng: chỉ còn hai path trực tiếp; evidencePersons = [P00, P01, P02] (không có
     * P10, P11), evidenceEdges chỉ gồm hai cạnh trực tiếp.
     * Phủ: giới hạn 3 đời áp dụng cả với đường thay thế tới một tổ tiên đã tới được.
     * BFS vẫn đi tới P10 (mức 2 cạnh) nhưng P10 không phải tổ tiên chung, nên không đi
     * vào bằng chứng. Đi cặp với test [8].
     */
    @Test
    void excludesLongerPathPastThreeGenerationsToAnAlreadyReachableAncestor() {
        Person ancestor = person("P00");
        Person first = person("P01");
        Person second = new Person("P02", Gender.FEMALE, "");
        List<ParentChildEdge> directEdges = List.of(
                new ParentChildEdge("P00", "P01"),
                new ParentChildEdge("P00", "P02")
        );
        List<ParentChildEdge> edges = new ArrayList<>(directEdges);
        edges.add(new ParentChildEdge("P00", "P10"));
        edges.add(new ParentChildEdge("P10", "P11"));
        edges.add(new ParentChildEdge("P11", "P01"));
        FamilyGraph graph = new FamilyGraph(
                List.of(ancestor, person("P10"), new Person("P11", Gender.FEMALE, ""), first, second),
                edges
        );

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph, new CheckRelationshipQuery("P01", "P02")
        );

        assertTrue(result.related());
        assertEquals(List.of("P00"), result.commonAncestorIds());
        assertEquals(List.of(
                new RelationshipPath("P01", "P00", List.of("P01", "P00")),
                new RelationshipPath("P02", "P00", List.of("P02", "P00"))
        ), result.paths());
        assertEquals(List.of(ancestor, first, second), result.evidencePersons());
        assertEquals(directEdges, result.evidenceEdges());
    }

    /**
     * [10] Khác giới nhưng không có tổ tiên chung (đồ thị không có cạnh nào).
     *
     * Kỳ vọng: không related, mọi danh sách rỗng.
     * Phủ: nhánh giao tập rỗng. Vì không có cạnh nên parentsByChild rỗng và mỗi người
     * chỉ có chính mình trong ancestorPaths.
     * Lưu ý: chưa có trường hợp hai người đều có tổ tiên nhưng hai tập không giao nhau.
     */
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

    /**
     * [11] WORST CASE: kịch bản nhiều path nhất mà BFS có thể sinh ra trong giới hạn 3 đời.
     *
     * <pre>
     *   P10  P11     P12  P13        ông bà (4 người khác nhau)
     *      \  /         \  /
     *      P20           P21         cha mẹ, cùng là cha mẹ của cả P01 lẫn P02
     *          \       /
     *        P01     P02             query (P01, P02), khác giới
     * </pre>
     *
     * Đồ thị có 8 người, 8 cạnh. Mỗi người có 7 path trong ancestorPaths (1 + 2 + 4),
     * trong đó 6 path tới tổ tiên chung. Path hạt giống [P01] và [P02] không tính vì
     * P01 và P02 không phải tổ tiên của nhau.
     *
     * Kỳ vọng: 6 tổ tiên chung (4 ông bà + 2 cha mẹ), 12 path, bằng chứng = toàn bộ graph.
     * Thứ tự path: theo tổ tiên đã sort (P10 ... P21), trong mỗi tổ tiên P01 trước P02.
     *
     * Lưu ý: đây là worst case về hằng số (số path và số tổ tiên chung), không làm đổi
     * độ phức tạp tổng thể O(V + E), vốn do các bước quét toàn bộ đồ thị chi phối.
     */
    @Test
    void handlesMaximumExpansionWithSixCommonAncestorsAndTwelvePaths() {
        List<Person> persons = List.of(
                person("P10"),
                new Person("P11", Gender.FEMALE, ""),
                person("P12"),
                new Person("P13", Gender.FEMALE, ""),
                person("P20"),
                new Person("P21", Gender.FEMALE, ""),
                person("P01"),
                new Person("P02", Gender.FEMALE, "")
        );
        List<ParentChildEdge> edges = List.of(
                new ParentChildEdge("P10", "P20"),
                new ParentChildEdge("P11", "P20"),
                new ParentChildEdge("P12", "P21"),
                new ParentChildEdge("P13", "P21"),
                new ParentChildEdge("P20", "P01"),
                new ParentChildEdge("P21", "P01"),
                new ParentChildEdge("P20", "P02"),
                new ParentChildEdge("P21", "P02")
        );
        FamilyGraph graph = new FamilyGraph(persons, edges);

        RelationshipResult result = RelationshipChecker.checkRelationship(
                graph, new CheckRelationshipQuery("P01", "P02")
        );

        assertTrue(result.related());
        assertEquals(
                List.of("P10", "P11", "P12", "P13", "P20", "P21"),
                result.commonAncestorIds()
        );
        assertEquals(List.of(
                new RelationshipPath("P01", "P10", List.of("P01", "P20", "P10")),
                new RelationshipPath("P02", "P10", List.of("P02", "P20", "P10")),
                new RelationshipPath("P01", "P11", List.of("P01", "P20", "P11")),
                new RelationshipPath("P02", "P11", List.of("P02", "P20", "P11")),
                new RelationshipPath("P01", "P12", List.of("P01", "P21", "P12")),
                new RelationshipPath("P02", "P12", List.of("P02", "P21", "P12")),
                new RelationshipPath("P01", "P13", List.of("P01", "P21", "P13")),
                new RelationshipPath("P02", "P13", List.of("P02", "P21", "P13")),
                new RelationshipPath("P01", "P20", List.of("P01", "P20")),
                new RelationshipPath("P02", "P20", List.of("P02", "P20")),
                new RelationshipPath("P01", "P21", List.of("P01", "P21")),
                new RelationshipPath("P02", "P21", List.of("P02", "P21"))
        ), result.paths());
        assertEquals(persons, result.evidencePersons());
        assertEquals(edges, result.evidenceEdges());
    }

    /**
     * Đồ thị mẫu: P00 là cha của hai người P01 và P02, giới tính truyền vào.
     * Dùng chung cho test [4] và [5].
     */
    private static FamilyGraph graphWithSiblings(Gender first, Gender second) {
        return new FamilyGraph(
                List.of(person("P00"), new Person("P01", first, ""), new Person("P02", second, "")),
                List.of(new ParentChildEdge("P00", "P01"), new ParentChildEdge("P00", "P02"))
        );
    }

    /** Tạo người giới tính MALE, tên rỗng. */
    private static Person person(String id) {
        return new Person(id, Gender.MALE, "");
    }
}