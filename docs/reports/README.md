# Bộ báo cáo kỹ thuật đồ thị gia phả

**Phiên bản source được mô tả:** commit `ba9b9c2920fc8621d1ab74e1681d7fa64540bdae`, checkout `feature/input-validation`.  
**Ngày kiểm chứng:** 17/10/2026. **Ngôn ngữ:** Java; source target 17, môi trường đã chạy JDK 25.0.2.  
**Mục đích:** giải thích kiến trúc, thuật toán, bằng chứng kiểm thử và giới hạn của implementation phục vụ báo cáo đại học và thuyết trình.

## Mục lục và lộ trình đọc

| Tài liệu | Câu hỏi được giải đáp |
|---|---|
| [00 — Tổng thể hệ thống](00_system_overview.md) | Bài toán là gì; vì sao là DAG; các object/module hợp tác thế nào; phần nào đã tích hợp? |
| [01 — Parse, Validate, Normalize](01_input_validation.md) | Raw text trở thành object ra sao; lỗi được chọn thế nào; DFS tìm cycle như thế nào? |
| [02 — Kahn, Family Tree, labeling](02_family_tree_topo_labeling.md) | Vì sao cha mẹ đứng trước con; cách giữ nhiều độ dài quan hệ và gán nhiều nhãn? |
| [03 — Relationship BFS](03_relationship_bfs.md) | Làm sao tìm mọi tổ tiên chung và mọi path 0–2 cạnh; evidence hình thành ra sao? |
| [04 — Kiểm thử và độ phức tạp](04_testing_and_complexity.md) | Điều gì đã chạy, còn thiếu gì, chi phí lý thuyết khác implementation ở đâu? |

Người trình bày tổng thể nên đọc 00 rồi 04. Người phụ trách thuật toán đọc báo cáo tương ứng và mở Input/Actual Output đi kèm. [Audit giai đoạn 1](../audits/week7-phase1-audit.md) giữ đầy đủ bảng **41 class**, đối chiếu contract và danh sách phát hiện ban đầu; không sao chép bảng đó vào mọi báo cáo.

## Quy ước quan trọng đã chốt

1. Cạnh luôn là **cha/mẹ → con**. Gia phả hợp lệ là DAG, có thể nhiều component và nhiều đường giữa hai người.
2. Family Tree k đời tính cả TARGET: mỗi hướng tối đa k−1 cạnh. BOTH là hợp hai lượt đơn hướng, không phải duyệt vô hướng.
3. Kahn dùng **FIFO**. Thứ tự PERSON quyết định queue root ban đầu; thứ tự EDGE và thời điểm indegree về 0 quyết định enqueue về sau. Không có ưu tiên PERSON toàn cục bằng heap.
4. Relationship giữ **tất cả path hợp lệ dài 0–2 cạnh** tới mỗi ancestor chung, kể cả path dài hơn shortest. Không có visited/distance/predecessor map trong implementation này.
5. `relationLevels` là tập các độ dài có dấu, không phải một khoảng cách ngắn nhất duy nhất hay tọa độ vẽ.

Các đoạn kế hoạch nhóm còn nói “một shortest path” được xem là nội dung chưa đồng bộ; bộ báo cáo theo [model contract](../contracts/model-contract.md), implementation và quyết định sau audit. Báo cáo không sửa các file contract/kế hoạch đó.

## Hiện trạng phải nêu khi trình bày

**Còn lỗi nghiệp vụ:** `RelationshipChecker.checkRelationship` trả false trước BFS nếu hai query person có cùng `Gender`, bao gồm UNKNOWN/UNKNOWN. Ví dụ chạy thực tế và expected đúng được trình bày tại [03, mục 8](03_relationship_bfs.md#8-phản-ví-dụ-đã-chạy-lỗi-lọc-giới-tính). Không mô tả toàn bộ checker là đúng với bài toán tìm huyết thống khi lỗi này chưa sửa.

**Còn thiếu tích hợp sản phẩm trong checkout:** `app`, `output`, `web` chỉ có file giữ chỗ; chưa có production dispatcher/serializer/CLI/web. Các ví dụ mới được chạy bằng **caller kiểm chứng riêng** gọi public API, không phải bằng application end-to-end đã tồn tại. Không có ảnh nào trong bộ báo cáo được gọi là screenshot web.

## Cách đọc bằng chứng

| Nhãn | Nghĩa |
|---|---|
| Expected theo đặc tả | Kết quả phải có nếu tuân thủ quy ước của bài toán |
| Actual API | Giá trị thực sự đọc từ object do production API trả khi chạy [script kiểm chứng](evidence/run_examples.jsh) |
| Test JUnit đã chạy | Assertion nằm trong bộ test hiện có đã chạy thành công; không mặc định mọi output đều được test đó kiểm tra |
| Trace tay | Trạng thái queue/map/indegree được dựng lại bằng đọc code trên fixture đã chỉ định; production code không xuất trace này |
| Chưa xác minh | Chưa có phép chạy/assertion tương ứng; không diễn giải thành thành công |

**Bằng chứng tách biệt:** [Maven 67 test](evidence/test_run_2026-10-17.txt), [21 lượt chạy ví dụ API](evidence/examples-run.log), [Actual Output JSON](evidence/actual), [snapshot source và SHA-256](evidence/source-snapshot.json). Hai ví dụ gender bug được ghi nhận là **khác expected nghiệp vụ**, không được cộng thành test đúng đặc tả. JSON là định dạng quan sát object của script báo cáo, không phải raw Output contract của sản phẩm.

## Hình và khả năng tái tạo

Hình được nhúng tương đối từ `figures/`; mỗi hình có source trong [figures/source](figures/source). Mermaid flowchart/sequence nằm ngay trong Markdown để chỉnh sửa trực tiếp. Các SVG gia phả lấy người, cạnh và nhãn từ fixture/Actual API; sơ đồ minh họa giữ chiều cạnh gốc, còn PATH được đọc từ nguồn lên ancestor.

Môi trường không có Graphviz `dot` trên PATH hoặc các vị trí cài đặt thông dụng đã kiểm tra. Vì vậy SVG được dựng bằng renderer Python dùng thư viện chuẩn, bố cục cố định; mỗi graph đồng thời xuất DOT để chỉnh sửa/render bằng Graphviz khi có. Không cài dependency mới. [Hướng dẫn nguồn hình](figures/source/README.md) nêu chính xác cách dựng lại và giới hạn bố cục.

## Thuật ngữ thống nhất

| Thuật ngữ | Trong project |
|---|---|
| TARGET / source | Người trung tâm của Family Tree / một trong hai người Relationship |
| `relationLevel` | Độ dài đường đơn hướng có dấu so với target: dương tổ tiên, âm hậu duệ |
| `greatDegree` | Với độ dài d≥3, degree=d−2; phân biệt các bậc cụ/chắt |
| `topoOrder` | Thứ tự cha/mẹ trước con; không phải hàng thế hệ |
| Common ancestor | ID đạt được từ cả hai nguồn trong cửa sổ 0–2 cạnh, bao gồm nguồn theo quy ước |
| Evidence | Hợp người/cạnh nằm trên các PATH thực sự trả về |
| `RELATED false` | Theo đặc tả: chưa tìm thấy tổ tiên chung trong cửa sổ và dữ liệu hiện có; hiện còn có thể do gender guard sai |

## Tài liệu tham khảo

Nguồn chính để kết luận hành vi là source và test của project. Tham khảo thuật toán ngoài project được liên kết tại các báo cáo tương ứng: source chính thức của thư viện giáo trình **Algorithms, 4th Edition** do Robert Sedgewick và Kevin Wayne duy trì. Không lấy implementation tham khảo làm bằng chứng chương trình này đã chạy đúng.

**Công việc còn lại trước thuyết trình:** xử lý lỗi gender trong một thay đổi được cho phép; hoàn thiện hoặc xác định nơi có bản tích hợp; bổ sung regression/coverage còn thiếu; chạy lại bằng chứng khi source thay đổi. Bộ báo cáo hiện mô tả trung thực phiên bản chưa sửa.
