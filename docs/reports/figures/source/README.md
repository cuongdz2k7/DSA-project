# Nguồn hình và tái tạo

[Chỉ mục báo cáo](../../README.md)

`render_figures.py` dùng Python standard library để đọc JSON Actual API tại `../../evidence/actual/`, dựng SVG tại thư mục cha và DOT tại đây. Script không phụ thuộc thư mục làm việc; lệnh tương đối sau chạy từ Maven project root:

```text
python docs/reports/figures/source/render_figures.py
```

Các vị trí node được đặt cố định cho tính dễ đọc; script không suy topo thành hàng thế hệ và không triển khai lại thuật toán Java. Hình family lấy người/cạnh/mức/nhãn từ result; hình evidence lấy đúng evidence paths/edges; hình invalid và boundary được ghi rõ là Input hoặc vùng tìm kiếm. `validation_errors.svg` rút gọn điểm gây lỗi của ba fixture riêng, không biểu diễn toàn bộ ba document.

Máy không có Graphviz `dot` tại thời điểm tạo. SVG được xuất trực tiếp để không phụ thuộc cài đặt thêm; DOT giữ ngữ nghĩa node/edge, có thể render bằng Graphviz khi có (bố cục có thể thay đổi). Flowchart, class diagram và sequence diagram Mermaid nằm trong Markdown để chỉnh sửa trực tiếp, không phụ thuộc các DOT gia phả.

`figure_manifest.json` lưu nội dung node/edge, màu, tọa độ và tên snapshot nguồn của mỗi hình. Hình là giải thích từ dữ liệu đã quan sát, không phải screenshot web hoặc log thuật toán. Preview PNG/contact sheet trong `../../evidence/visual-qa/` chỉ dùng kiểm tra layout của SVG; không được coi là output ứng dụng.
