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

## Hướng dẫn chạy và kiểm thử

### Bài Tập 1
```bash
cd BaiTap1
./gradlew test
```
