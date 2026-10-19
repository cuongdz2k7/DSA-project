# 04 — Kiểm thử tổng hợp, độ phức tạp và phạm vi đáp ứng

[Chỉ mục](README.md) · [← Relationship BFS](03_relationship_bfs.md) · [Tổng thể](00_system_overview.md)

**Snapshot source:** `ba9b9c2920fc8621d1ab74e1681d7fa64540bdae`. **Ngày kiểm chứng:** 17/10/2026. Kết quả dưới đây phân biệt test suite hiện có, các lượt quan sát API mới và trace tay.

## Mục lục

1. [Phương pháp và nguồn bằng chứng](#1-phương-pháp-và-nguồn-bằng-chứng)
2. [Kết quả Maven thực tế](#2-kết-quả-maven-thực-tế)
3. [Các ví dụ API đã chạy](#3-các-ví-dụ-api-đã-chạy)
4. [Kịch bản xuyên các module](#4-kịch-bản-xuyên-các-module)
5. [So sánh độ phức tạp](#5-so-sánh-độ-phức-tạp)
6. [Phần chưa xác minh và rủi ro còn mở](#6-phần-chưa-xác-minh-và-rủi-ro-còn-mở)
7. [Đối chiếu kế hoạch tuần 7](#7-đối-chiếu-kế-hoạch-tuần-7)
8. [Tái lập và kiểm tra tài liệu](#8-tái-lập-và-kiểm-tra-tài-liệu)

## 1. Phương pháp và nguồn bằng chứng

| Loại bằng chứng | Dùng để kết luận điều gì | Giới hạn |
|---|---|---|
| Đọc toàn bộ main/test source và contract | Xác định hành vi, tiền điều kiện, invariant, giới hạn | Không thay cho thực thi mọi tình huống |
| `mvn clean test` trong giai đoạn1 | Source/test compile lại; assertion hiện có pass | Test có thể kỳ vọng sai yêu cầu, hoặc thiếu assertion |
| 21 raw fixtures qua caller JShell riêng | Actual parse/validate/result trên ví dụ dùng trong báo cáo | Không tự trở thành JUnit regression hay production entry point |
| Trace tay | Giải thích queue/map/indegree theo source | Không phải log instrumentation từ chương trình |
| SVG/DOT và kiểm tra liên kết | Hình khớp fixture/result, đường dẫn mở được | Không chứng minh layout web, vì web chưa có |

Nguồn snapshot: [SHA-256 các file Java/test/contract](evidence/source-snapshot.json). Caller [run_examples.jsh](evidence/run_examples.jsh) chỉ đọc example rồi gọi production API; không cài lại BFS/Kahn/validator và không sửa dữ liệu để “làm xanh”. JSON được xuất bằng đọc component record và các getter liên quan, là định dạng bằng chứng riêng.

## 2. Kết quả Maven thực tế

Lệnh đã chạy trong giai đoạn1: `mvn clean test`, tại Maven project root. Kết quả **BUILD SUCCESS**, exit code0; compile41 file main và14 file test; **67 invocation, failures0, errors0, skipped0**. Kết thúc17:32:00 ngày17/10/2026 (+07:00); Maven báo3.968s cho build/test. Không dùng thời gian này làm benchmark graph.

Runtime Surefire ghi **JDK25.0.2**, `pom.xml` compiler release17. Chưa chạy trên runtime17. Console trước Maven có dòng `Access is denied.` chưa rõ nguồn, nhưng compile/suite/XML xác nhận hoàn tất thành công. [Biên bản lưu lại](evidence/test_run_2026-10-17.txt) ghi cả ghi chú này.

Không chạy lại suite chỉ vì thêm Markdown/SVG: source/test không đổi so với lần clean build; file hash được đối chiếu ở cuối công việc. Nếu sửa Java sau này, cần chạy lại suite và các ví dụ; không tái sử dụng snapshot này như kết quả phiên bản mới.

### 2.1. Danh sách unit test và tích hợp nội bộ

| Suite | Invocation | Nội dung / phạm vi |
|---|---:|---|
| [InputNormalizerTest](../../src/test/java/com/familygraph/input/InputNormalizerTest.java) | 2 | Chuyển hai query, enum, giữ input order |
| [InputPipelineModelsTest](../../src/test/java/com/familygraph/input/InputPipelineModelsTest.java) | 2 | Một số trạng thái data/error mâu thuẫn |
| [ParsedInputValidatorTest](../../src/test/java/com/familygraph/input/ParsedInputValidatorTest.java) | 6 | Nhóm mã lỗi, parent combinations, ưu tiên cycle/duplicate |
| [RawInputParserTest](../../src/test/java/com/familygraph/input/RawInputParserTest.java) | 18 | Tích hợp parser→validator→normalizer; không gọi query |
| [RawTextParserTest](../../src/test/java/com/familygraph/input/RawTextParserTest.java) | 4 | Grammar/token/name/whitespace, malformed và list immutable |
| [FamilyTreeBuilderTest](../../src/test/java/com/familygraph/familytree/FamilyTreeBuilderTest.java) | 4 | Hướng/biên/missing parent/nhiều levels |
| [FamilyTreePipelineTest](../../src/test/java/com/familygraph/familytree/FamilyTreePipelineTest.java) | 1 | Kahn→builder→labeler; không parse raw Input |
| [LabelFamilyGraphTest](../../src/test/java/com/familygraph/familytree/LabelFamilyGraphTest.java) | 1 | SELF/FATHER/SON cơ bản |
| [TopologicalSorterTest](../../src/test/java/com/familygraph/familytree/TopologicalSorterTest.java) | 3 | Basic/root order/disconnected |
| [FamilyTreeModelsTest](../../src/test/java/com/familygraph/model/FamilyTreeModelsTest.java) | 5 | Model copy, levels, một số code/offset |
| [GraphAndQueryModelsTest](../../src/test/java/com/familygraph/model/GraphAndQueryModelsTest.java) | 3 | Copy list; DTO chưa validate vẫn tạo được |
| [RelationshipModelsTest](../../src/test/java/com/familygraph/model/RelationshipModelsTest.java) | 3 | Model path/evidence/related, không chạy checker |
| [ValidationModelsTest](../../src/test/java/com/familygraph/model/ValidationModelsTest.java) | 3 | Trạng thái validation và immutable cycle |
| [RelationshipCheckerTest](../../src/test/java/com/familygraph/model/relationship/RelationshipCheckerTest.java) | 12 | Common ancestors, direct, boundaries, multi-path; có expectation sai về query cùng gender |
| **Tổng** | **67** | **0 failure, 0 error, 0 skipped** |

RelationshipCheckerTest có10 method nhưng12 invocation do hai parameterized method có hai tổ hợp mỗi method. Một số method có nhiều tình huống/assertion; không dùng67 làm số yêu cầu hoặc tỷ lệ coverage. Không có JaCoCo/coverage report trong project.

### 2.2. Vì sao xanh chưa đủ

`returnsUnrelatedForSameKnownGenderEvenWithCommonAncestor` kỳ vọng false cho hai sibling M/M hoặc F/F. Hành vi code phù hợp test nên pass, nhưng khác bài toán giao tập tổ tiên. Chất lượng kiểm thử còn phụ thuộc correctness của oracle expected, không chỉ khả năng chương trình đạt oracle đó.

Do đó cần báo cáo đồng thời **67 test pass** và **lỗi nghiệp vụ đã tái hiện**. Không được bỏ một trong hai để tạo cảm giác project hoàn chỉnh hơn hoặc lỗi build nghiêm trọng hơn thực tế.

## 3. Các ví dụ API đã chạy

Caller chạy bằng JShell của JDK25.0.2, `--execution local`, dùng `target/classes` được clean build ở trên. [Log](evidence/examples-run.log) có21 dòng OBSERVED. Sáu fixture bị validator từ chối đúng dự kiến;15 fixture valid đi tới module query. Trong15 fixture valid có hai phản ví dụ gender bug.

**Kết quả nghiệp vụ:** 19/21 ví dụ khớp expected đã mô tả; **2/21 có sai lệch đã biết**. Đây là thống kê fixture minh họa được chọn, không phải tỷ lệ chính xác trên dữ liệu thực tế hay coverage phần mềm. [Kiểm tra expected/actual bằng máy](evidence/example-checks.json) lưu từng trường hợp.

### 3.1. Family Tree — 7 fixture

| Input | Expected then chốt | Actual đã quan sát | Chứng minh |
|---|---|---|---|
| [Cơ bản](examples/family_tree_example.txt) | P03k2BOTH: FATHER/MOTHER/SELF/DAUGHTER | [Khớp](evidence/actual/family_tree_example.json), đủ3edge | Pipeline và hai hướng |
| [Đa mức ancestor](examples/family_tree_multiple_levels.txt) | P10 levels[1,3], FATHER/GREAT_GRANDFATHER_1 | [Khớp](evidence/actual/family_tree_multiple_levels.json) | Không chỉ shortest level |
| [Đa mức descendant](examples/family_tree_descendant_multiple_levels.txt) | P13 levels[−1,−3], SON/GREAT_GRANDSON_1 | [Khớp](evidence/actual/family_tree_descendant_multiple_levels.json) | Nhiều mức âm |
| [k1](examples/family_tree_one_generation.txt) | ChỉP03 SELF, khôngedge | [Khớp](evidence/actual/family_tree_one_generation.json) | Biên D0 |
| [FIFO](examples/topo_fifo.txt) | Topo[A,C,B] theo EDGEenqueue | [Khớp](evidence/actual/topo_fifo.json) | Không ưu tiên PERSON toàn cục |
| [Diamond](examples/family_tree_diamond.txt) | A mức[2], nhãnGRANDPARENT, giữ4edge | [Khớp](evidence/actual/family_tree_diamond.json) | Dedupe state, giữ cạnh, UNKNOWN |
| [Union edges](examples/family_tree_union_edges.txt) | A/B[1,2], khônglevel3, giữ6edge | [Khớp](evidence/actual/family_tree_union_edges.json) | Không suy nhãn từ đường ghép ngoài biên |

### 3.2. Relationship — 8 fixture

| Input | Expected nghiệp vụ | Actual API | Trạng thái |
|---|---|---|---|
| [Hai ancestor](examples/relationship_example.txt) | true, commonP01/P02,4path | [Khớp](evidence/actual/relationship_example.json) | Đã chạy |
| [Nhiều shortest path](examples/relationship_multiple_paths.txt) | true, commonP00,4path | [Khớp](evidence/actual/relationship_multiple_paths.json) | Đã chạy |
| [Short + long](examples/relationship_short_and_long.txt) | true,3path dài1/2/1 | [Khớp](evidence/actual/relationship_short_and_long.json) | Đã chạy |
| [Direct](examples/relationship_direct.txt) | true, mộtpath0/mộtpath1 | [Khớp](evidence/actual/relationship_direct.json) | Đã chạy |
| [Ngoài biên](examples/relationship_outside_boundary.txt) | false; common chỉ ởdepth3 | [Khớp](evidence/actual/relationship_outside_boundary.json) | Đã chạy |
| [Disconnected](examples/relationship_disconnected.txt) | false; khôngedge | [Khớp](evidence/actual/relationship_disconnected.json) | Đã chạy |
| [M/M có common parent](examples/relationship_same_gender_bug.txt) | **true**, commonP00 | [**false**, commonrỗng](evidence/actual/relationship_same_gender_bug.json) | **Sai đặc tả** |
| [U/U có common parent](examples/relationship_unknown_gender_bug.txt) | **true**, commonP00 | [**false**, commonrỗng](evidence/actual/relationship_unknown_gender_bug.json) | **Sai đặc tả** |

### 3.3. Validation — 6 fixture

| Input | Expected code/detail quan trọng | Actual |
|---|---|---|
| [Count](examples/invalid_count.txt) | INVALID_COUNT: expected2/found1 | [Khớp](evidence/actual/invalid_count.json) |
| [Cycle](examples/invalid_cycle_example.txt) | DIRECTED_CYCLE, closed[P01,P02,P03,P01] | [Khớp](evidence/actual/invalid_cycle_example.json) |
| [Duplicate](examples/invalid_duplicate_edge.txt) | DUPLICATE_EDGE trước parent conflict | [Khớp](evidence/actual/invalid_duplicate_edge.json) |
| [Parent gender](examples/invalid_parent_gender.txt) | SAME_GENDER_PARENTS, P01/P02 cùngMALE | [Khớp](evidence/actual/invalid_parent_gender.json) |
| [Quote](examples/invalid_quote.txt) | MALFORMED_INPUT, detail `invalid PERSON name` | [Khớp](evidence/actual/invalid_quote.json) |
| [Unknown ID](examples/invalid_unknown_id.txt) | UNKNOWN_ID P99 từedge trước P88query | [Khớp](evidence/actual/invalid_unknown_id.json) |

## 4. Kịch bản xuyên các module

Chọn [family_tree_multiple_levels.txt](examples/family_tree_multiple_levels.txt), input đủ bốn người/bốn cạnh và queryP13,k4,ANCESTORS. Đây là kịch bản đã chạy **raw text → input facade → algorithm APIs → result object → snapshot JSON** bằng caller kiểm chứng. Chưa có production raw Output/web cuối để chạy.

| Bước | Method/object thực tế | Giá trị quan trọng |
|---|---|---|
| 1 | `RawTextParser.parse` → ParseResult | successful=true; count4/4; query tokens[P13,"4",ANCESTORS] |
| 2 | `ParsedInputValidator.validate` → GraphValidationResult | valid=true; DAG, parent rule/query đều đạt |
| 3 | `RawInputParser.parseAndValidate` → ValidationResult | data=NormalizedInput; enum/int đã chuyển; error=null |
| 4 | `TopologicalSorter.topologicalSort` | topo[P10,P11,P12,P13] |
| 5 | `FamilyTreeBuilder.buildFamilyView` | P10[1,3], P11[2], P12[1], P13[0]; đủ4edge |
| 6 | `FamilyTreePipeline.createFamilyTree` | P10 FATHER/GREAT_GRANDFATHER_1; P11 GRANDFATHER; P12 MOTHER; P13 SELF |
| 7 | Caller ghi snapshot | [Actual JSON đầy đủ](evidence/actual/family_tree_multiple_levels.json), giữ records và getter codes/offsets |

Caller có gọi sorter/builder riêng để quan sát intermediate, rồi gọi pipeline để nhận result cuối; đó là quan sát lặp có chủ đích, không phải sequence production gọi sorter hai lần bên trong pipeline. [Sequence thật](00_system_overview.md#6-sequence-các-lời-gọi) mô tả pipeline chỉ chạy mỗi bước một lần.

Nếu serialize theo kế hoạch sau này, các PERSON/EDGE/PATH cần đúng format/tên trường đã quy định. Báo cáo không tạo một chuỗi “actual raw output” thay cho serializer còn thiếu. Với input invalid, snapshot không có query result vì caller chỉ dispatch khi valid; đây là bằng chứng của caller kiểm chứng, không thay integration test production.

## 5. So sánh độ phức tạp

### 5.1. Biến và giả định

V/E là người/cạnh toàn graph; L tổng ký tự Input; k số đời Family; h=min(k−1,V−1); S số state(ID,level); T số lượt xét cạnh từ state; q_v số mức của v, Q=Σq_v; C common ancestor count. Hash lookup giả định kỳ vọng/amortized O(1), ID có độ dài bị chặn. Không có dữ liệu thực nghiệm đủ để công bố Big-O trung bình theo một phân bố đầu vào.

| Module | Thời gian theo implementation | Không gian gồm chỉ mục/lưu trữ phụ trợ |
|---|---|---|
| Parse/validate/normalize | O(L+V+E), với grammar/token/hash như giả định | O(L+V+E); DFS stack O(V) |
| Kahn riêng | O(V+E) | O(V+E), adjacency được lập trong hàm |
| Family builder | O(V+E+S+T+Σq_v log(q_v+1)) | O(V+E+S+Q) |
| Labeler | O(V_s+E_s+Q+Σq_v log(q_v+1)) upper bound | O(V_s+E_s+Q), kể cả snapshot list |
| Relationship fixeddepth2 | O(V+E+C log(C+1)) | O(V+E), path có tối đa3ID |

Chi tiết proof và input gây chi phí cao: [01, mục7](01_input_validation.md#7-độ-phức-tạp), [02, mục8](02_family_tree_topo_labeling.md#8-độ-phức-tạp), [03, mục9](03_relationship_bfs.md#9-độ-phức-tạp). Không dùng O(V+E) của Kahn thay cho toàn Family Tree; một vertex có thể được xử lý tại nhiều level. Cũng không gán sự bùng nổ all-paths với depth tùy ý cho checker đang cố định2 cạnh.

### 5.2. Ghép chi phí theo một raw query

Family Tree có S=O(V(h+1)), T=O(Eh), Q=O(V(h+1)); tổng cả input và pipeline có cận trên:

$$
O\left(L+V+E+V(h+1)+Eh+V(h+1)\log(h+2)\right).
$$

Relationship trong nhánh BFS có tổng O(L+V+E+C log(C+1)); trả sớm vì guard sai không được xem là một tối ưu hợp lệ của bài toán. Cả hai tuyến đều trả thêm dữ liệu output theo kích thước result; chưa phân tích serializer/web chưa tồn tại.

Graph gốc/list domain dùng O(V+E); input còn giữ text và parsed records trong giai đoạn xử lý. Trong một lần query Family, state/mức có thể lớn hơn số người; trong Relationship depth2, path có độ dài hằng nên tổng lưu path bị chặn theo E. Các chỉ mục hiện được lập lại mỗi lần gọi, chưa có tiền xử lý được cache một lần cho nhiều query.

Worst-case cấu trúc: DAG dày có nhiều cạnh tắt làm Family xét lại adjacency ở nhiều mức; DAG sâu làm DFS stack dài; nhiều UNKNOWN parent/grandparent làm Relationship có nhiều path/C. Không có số đo RAM/thời gian để gắn nhãn benchmark cho các cấu trúc này.

## 6. Phần chưa xác minh và rủi ro còn mở

| Nội dung | Đã có bằng chứng | Còn thiếu / kết luận giới hạn |
|---|---|---|
| Labels UNKNOWN, nhiều mức, negative great | Một số nhánh đã chạy qua fixture mới | Chưa có toàn ma trận gender×level và regression JUnit cho mọi nhánh |
| Error priority | Một số cặp lỗi đã có JUnit/API | Chưa mọi cặp; overflow/arity/raw line edge cases chưa đầy đủ |
| Cycle extraction | Cycle đơn giản và priority đã chạy | Cycle có prefix ngoài vòng/component cuối chưa có fixture riêng |
| Validation safety | Facade dừng đúng ở fixture invalid | Tự tạo parsed/validated DTO sai vẫn có thể lỗi exception |
| Deep graph | Proof DFS tuyến tính và source đệ quy | Chưa stress stack, chưa công bố giới hạn kích thước |
| BFS gender | Hai phản ví dụ mới, M/M/F/F trong JUnit | Lỗi còn trong source; không thể kết luận đúng mọi query |
| Ordering | FIFO/fixtures chạy thật, sort common IDs | Chưa test hoán vị input có kiểm soát toàn bộ kết quả |
| Tích hợp/web | Caller script ghép API thành công trên fixture | Chưa có production entry point/serializer/web/integration test |
| Môi trường | Compile release17 trên JDK25.0.2 | Chưa test runtime17; README yêu cầu25+ còn lệch plan/pom |

Không đổi constructor record để tự validate lại toàn bộ vì hợp đồng giao invariant cho pipeline. Nếu cần tăng độ an toàn API public, đó là quyết định thiết kế riêng, không phải giả định các guard đã tồn tại.

## 7. Đối chiếu kế hoạch tuần 7

| Mốc/yêu cầu kế hoạch | Trạng thái trong checkout | Căn cứ |
|---|---|---|
| Parser, validator, normalized object | Có | Source/test/API fixtures |
| Kahn, graph con, relationLevels, labeling | Có | FamilyTreePipeline và7 fixture mới |
| BFS, common ancestor, evidence paths | Có, nhưng còn lỗi query gender | Checker/test và hai phản ví dụ thực thi |
| Output thô thống nhất | Chưa có serializer | Package output chỉ `.gitkeep` |
| Chạy từ raw Input tới output sản phẩm | Chưa có production app | Script chỉ là caller báo cáo |
| Web gia phả và tô PATH | Chưa có trong checkout | web/css/js chỉ `.gitkeep` |
| Trace/độ phức tạp/hình cho thuyết trình | Có trong bộ báo cáo này | Markdown, SVG, DOT, fixture và bằng chứng |
| Slide và diễn tập | Chưa thực hiện trong nhiệm vụ này | `docs/presentation` chưa có deck |

Trước demo nên ưu tiên sửa gender guard và test oracle; xác định/hoàn thiện bản tích hợp; thêm regression cho các fixture báo cáo; chạy lại snapshot sau sửa; sau đó chọn kịch bản trình bày gồm F01/F02, R01/R03 và một input cycle. Hướng dẫn này là thứ tự công việc, không phải xác nhận các việc sửa đã được làm.

## 8. Tái lập và kiểm tra tài liệu

Từ project root, trên môi trường Java/Maven phù hợp:

1. `mvn clean test`: kỳ vọng build/test theo source hiện có; suite có thể vẫn pass dù gender bug vì oracle đang sai.
2. Gọi JShell với classpath `target/classes`, execution local và file [run_examples.jsh](evidence/run_examples.jsh): đọc21 fixture, xuất từng Actual JSON và dòng OBSERVED. Windows có thể cần quyền Java Preferences; lần chạy bằng chứng đã được cho phép sau khi sandbox chặn registry.
3. Chạy [render_figures.py](figures/source/render_figures.py) bằng Python3: dựng12 SVG/DOT từ snapshot. Script chỉ vẽ dữ liệu, không thay API.
4. Chạy [verify_reports.py](evidence/verify_reports.py): kiểm tra expected/actual, liên kết/anchor/ảnh, cấu trúc SVG, snippet và source hash. **Hai sai lệch gender phải được ghi nhận**, không bị đổi expected để thành khớp nghiệp vụ.

Kết quả kiểm tra artifact tại [quality-checks.json](evidence/quality-checks.json). Các lần kiểm tra không thay source/test/contract. Source hash hoặc snapshot đổi phải dẫn tới xem lại báo cáo; không dùng tài liệu như bằng chứng của một commit khác mà không chạy lại.
