# DSA Project - Đồ thị gia phả

Project Java sử dụng đồ thị có hướng để xây dựng gia phả và kiểm tra quan hệ trong phạm vi ba đời.

## Phân chia package

- `model`: các object dữ liệu dùng chung giữa các nhóm.
- `graph`: cấu trúc hỗ trợ và chỉ mục đồ thị dùng chung.
- `input`: nhóm 3 đọc, kiểm tra và chuẩn hóa raw input.
- `familytree`: nhóm 1 xử lý topo, truy vấn gia phả, `LEVEL` và nhãn quan hệ.
- `relationship`: nhóm 2 xử lý BFS và kiểm tra tổ tiên chung trong ba đời.
- `output`: chuyển Java object thành output thô.
- `app`: điểm chạy và luồng tích hợp chung.
- `web`: phần hiển thị được tích hợp ở giai đoạn cuối.

## Quy ước làm việc

- Chỉ nhóm 3 đọc và kiểm tra raw input.
- Nhóm 1 và nhóm 2 chỉ nhận `NormalizedInput` hoặc các Java object đã được xác nhận hợp lệ.
- Không tự ý thay đổi package `model`. Mọi thay đổi hợp đồng dữ liệu phải được ba nhóm thống nhất và thực hiện qua pull request riêng.
- Không commit thư mục `target` hoặc file cấu hình riêng của IDE.

## Kiểm tra project

Yêu cầu Java 17 trở lên và Maven 3.9 trở lên.

```bash
mvn test
```

Đặc tả đầy đủ nằm trong [kế hoạch làm việc](./Ke_hoach_chinh_sua_nhom.md).
