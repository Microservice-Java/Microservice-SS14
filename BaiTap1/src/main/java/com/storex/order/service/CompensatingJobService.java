package com.storex.order.service;

import com.storex.order.client.InventoryClient;
import com.storex.order.model.FailedCompensationLog;
import com.storex.order.repository.FailedCompensationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensatingJobService {

    private final InventoryClient inventoryClient;
    private final FailedCompensationLogRepository compensationLogRepository;

    /**
     * Job chạy ngầm định kỳ quét và retry hoàn kho bù trừ cho các giao dịch bị lỗi kép
     */
    @Scheduled(fixedDelay = 10000)
    public void retryFailedCompensations() {
        List<FailedCompensationLog> pendingLogs = compensationLogRepository.findByStatus("PENDING_RETRY");

        if (pendingLogs.isEmpty()) {
            return;
        }

        log.info("[BACKGROUND SAGA JOB] Found {} pending compensation retries in database.", pendingLogs.size());

        for (FailedCompensationLog logRecord : pendingLogs) {
            try {
                log.info("Retrying stock restoration for logId={}, productId={}, quantity={}", logRecord.getId(), logRecord.getProductId(), logRecord.getQuantity());

                boolean restored = inventoryClient.increaseStock(logRecord.getProductId(), logRecord.getQuantity());

                if (restored) {
                    logRecord.setStatus("RESOLVED");
                    logRecord.setLastRetryAt(LocalDateTime.now());
                    compensationLogRepository.save(logRecord);
                    log.info("[BACKGROUND SAGA JOB SUCCESS] Successfully compensated logId={}! Stock restored.", logRecord.getId());
                } else {
                    logRecord.setRetryCount(logRecord.getRetryCount() + 1);
                    logRecord.setLastRetryAt(LocalDateTime.now());
                    if (logRecord.getRetryCount() >= 5) {
                        logRecord.setStatus("FAILED_PERMANENTLY");
                        log.error("[BACKGROUND SAGA JOB FAILED] LogId={} reached max retries (5). Marked FAILED_PERMANENTLY for manual audit.", logRecord.getId());
                    }
                    compensationLogRepository.save(logRecord);
                }
            } catch (Exception ex) {
                log.error("[BACKGROUND SAGA JOB ERROR] Failed retry attempt for logId={}: {}", logRecord.getId(), ex.getMessage());
                logRecord.setRetryCount(logRecord.getRetryCount() + 1);
                logRecord.setLastRetryAt(LocalDateTime.now());
                if (logRecord.getRetryCount() >= 5) {
                    logRecord.setStatus("FAILED_PERMANENTLY");
                }
                compensationLogRepository.save(logRecord);
            }
        }
    }
}
