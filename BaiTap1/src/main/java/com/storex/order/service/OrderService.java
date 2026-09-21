package com.storex.order.service;

import com.storex.order.client.InventoryClient;
import com.storex.order.dto.OrderRequest;
import com.storex.order.model.FailedCompensationLog;
import com.storex.order.model.Order;
import com.storex.order.repository.FailedCompensationLogRepository;
import com.storex.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;
    private final FailedCompensationLogRepository compensationLogRepository;

    /**
     * Tạo đơn hàng với cơ chế Bù Trừ (Compensating Transaction) chuẩn Saga Pattern
     */
    public ResponseEntity<Order> createOrder(OrderRequest request) {
        log.info("Starting order creation saga for productId={}, quantity={}", request.getProductId(), request.getQuantity());

        // 1. Gọi Inventory Service để trừ kho
        boolean stockDecreased = inventoryClient.decreaseStock(request.getProductId(), request.getQuantity());
        if (!stockDecreased) {
            log.warn("Decrease stock failed: Product out of stock. ProductId={}", request.getProductId());
            return ResponseEntity.badRequest().body(null); // Hết hàng
        }

        log.info("Successfully decreased stock for productId={}. Proceeding to save order...", request.getProductId());

        // 2. Tạo đối tượng đơn hàng
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus("PENDING");

        // 3. Lưu đơn hàng vào DB với cơ chế bù trừ
        try {
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved successfully with orderId={}", savedOrder.getId());
            return ResponseEntity.ok(savedOrder);
        } catch (Exception e) {
            log.error("CRITICAL ERROR: Save order failed due to: {}. Triggering Compensating Transaction (Restore Stock)...", e.getMessage());

            // BƯỚC BÙ TRỪ (COMPENSATING TRANSACTION): Hoàn lại số lượng kho khi tạo đơn thất bại
            boolean stockRestored = false;
            try {
                stockRestored = inventoryClient.increaseStock(request.getProductId(), request.getQuantity());
            } catch (Exception compensateEx) {
                log.error("COMPENSATE API FAILED (Network/Timeout): Cannot reach InventoryClient to restore stock!", compensateEx);
            }

            if (stockRestored) {
                log.info("COMPENSATED SUCCESSFULLY: Restored {} units of productId={} to inventory.", request.getQuantity(), request.getProductId());
            } else {
                // XỬ LÝ LỖI KÉP (DOUBLE FAILURE): Khi hoàn kho thất bại -> Lưu log vào DB để Scheduled Retry Job xử lý
                log.error("DOUBLE FAILURE: Stock compensation failed! Logging compensation event for background retry job.");
                saveFailedCompensationLog(request, e.getMessage());
            }

            return ResponseEntity.status(500).build();
        }
    }

    private void saveFailedCompensationLog(OrderRequest request, String reason) {
        try {
            FailedCompensationLog logRecord = FailedCompensationLog.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .reason(reason)
                    .status("PENDING_RETRY")
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();
            compensationLogRepository.save(logRecord);
            log.info("Saved FailedCompensationLog record into DB for productId={}", request.getProductId());
        } catch (Exception ex) {
            log.error("FATAL: Failed to save FailedCompensationLog into DB!", ex);
        }
    }
}
