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

### [Bài Tập 3: Thiết Kế Vũ Điệu Choreography Saga](./BaiTap3)
- **Mục tiêu**: Phân tích I/O, thiết kế lưu đồ (Flowchart) và mô phỏng chuỗi giao dịch phân tán giữa 3 dịch vụ `Order` $\rightarrow$ `Payment` $\rightarrow$ `Shipping` theo mô hình Choreography Saga.
- **Giải pháp**:
  - Xây dựng hệ thống Event Bus phân tán mô phỏng Message Broker (Kafka/RabbitMQ).
  - Triển khai Happy Path: `OrderCreatedEvent` $\rightarrow$ `PaymentSuccessEvent` $\rightarrow$ `ShippingSuccessEvent` $\rightarrow$ Order `COMPLETED`.
  - Triển khai Compensating Path khi giao hàng thất bại: `ShippingFailedEvent` $\rightarrow$ `CompensatePaymentEvent` $\rightarrow$ `RefundSuccessEvent` $\rightarrow$ Order `CANCELED`.
  - Kích hoạt tự động hoàn tiền & hủy đơn khi Shipping Service bị Timeout (>30s).
- **Báo cáo chi tiết**: [BaoCao_BaiTap3.md](./BaiTap3/BaoCao_BaiTap3.md)

---

### [Bài Tập 4: Sự Đánh Đổi Giữa Tự Do Và Tập Trung (Choreography vs Orchestration)](./BaiTap4)
- **Mục tiêu**: Phân tích việc tích hợp dịch vụ Voucher (Mã giảm giá), so sánh 2 mô hình Choreography và Orchestration Saga trên 5 tiêu chí kiến trúc, triển khai demo mô hình Orchestration Saga.
- **Giải pháp**:
  - Xây dựng bộ điều phối tập trung `OrderSagaOrchestrator` thực thi 4 bước tuần tự: `Order` $\rightarrow$ `Voucher` $\rightarrow$ `Payment` $\rightarrow$ `Shipping`.
  - Quản lý trạng thái tiến trình `SagaStateData` tập trung, giúp dễ dàng theo dõi (Observability) và trace lỗi.
  - Tự động kích hoạt bù trừ ngược chiều (Reverse Compensation) khi có bất kỳ bước nào thất bại (`Shipping Failed` $\rightarrow$ Refund Payment $\rightarrow$ Release Voucher $\rightarrow$ Cancel Order).
- **Báo cáo chi tiết**: [BaoCao_BaiTap4.md](./BaiTap4/BaoCao_BaiTap4.md)

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

### Bài Tập 3
```bash
cd BaiTap3
./gradlew test
```

### Bài Tập 4
```bash
cd BaiTap4
./gradlew test
```
