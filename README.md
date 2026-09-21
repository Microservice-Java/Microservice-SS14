# Microservice SS14 - Distributed Transactions & Saga Pattern

Repository lưu trữ bài tập thực hành về **Distributed Transactions (Giao dịch phân tán)** và **Saga Pattern (Compensating Transactions, Outbox Pattern)** trong kiến trúc Microservices.

## Danh sách bài tập

### [Bài Tập 1: Vá Lỗ Hổng "Kho Treo" (Rollback Logic trong Saga)](./BaiTap1)
- **Mục tiêu**: Phân tích hiện tượng "hàng ảo" (Data Inconsistency) khi thiếu bước Bù Trừ (Compensating Transaction) và tái cấu trúc mã nguồn `OrderService`.
- **Giải pháp**:
  - Triển khai bước hoàn kho bù trừ `increaseStock` khi tạo đơn hàng thất bại.
  - Giải quyết sự cố lỗi hoàn kho kép (Double Failure) bằng mô hình **Saga Log State + Scheduled Retry Job** (`FailedCompensationLog` & `CompensatingJobService`).
- **Báo cáo chi tiết**: [BaoCao_BaiTap1.md](./BaiTap1/BaoCao_BaiTap1.md)

---

### [Bài Tập 2: Đồng Bộ Trạng Thái "Đơn Hàng Lạc Lối"](./BaiTap2)
- **Mục tiêu**: Khắc phục tình trạng đơn hàng bị treo ở trạng thái `PENDING` khi xảy ra mất kết nối mạng ở thời điểm phản hồi từ Payment-Service hoặc không nhận được callback/event.
- **Giải pháp**:
  - Tái cấu trúc `PaymentEventListener` xử lý chuyển trạng thái an toàn & chống lặp (Idempotent State Transition).
  - Triển khai State Machine chuyển trạng thái đơn hàng (`PENDING` $\rightarrow$ `PAID` / `CANCELED` / `FAILED` $\rightarrow$ `SHIPPED`).
  - Triển khai `OrderTimeoutScheduler` kết hợp Active Reconcile API (`PaymentClient.checkPaymentStatus`) để chủ động đối soát các đơn hàng `PENDING` quá 5 phút, cập nhật trạng thái chuẩn và hoàn kho nếu thất bại.
- **Báo cáo chi tiết**: [BaoCao_BaiTap2.md](./BaiTap2/BaoCao_BaiTap2.md)

---

## Hướng dẫn chạy và kiểm thử

### Bài Tập 1
```bash
cd BaiTap1
./gradlew test
```

### Bài Tập 2
```bash
cd BaiTap2
./gradlew test
```
