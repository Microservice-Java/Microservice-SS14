package com.storex.saga.service;

import com.storex.saga.model.Voucher;
import com.storex.saga.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {

    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Transactional
    public Double applyVoucher(String code, Double orderAmount) {
        if (code == null || code.trim().isEmpty()) {
            log.info("No voucher code provided. Discount = 0.0");
            return 0.0;
        }

        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Voucher code '" + code + "' does not exist"));

        if (orderAmount < voucher.getMinOrderAmount()) {
            throw new IllegalArgumentException("Order amount " + orderAmount +
                    " does not meet minimum required amount " + voucher.getMinOrderAmount() + " for voucher " + code);
        }

        if (voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new IllegalStateException("Voucher '" + code + "' usage limit reached (" + voucher.getUsageLimit() + ")");
        }

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);
        log.info("Voucher '{}' applied successfully! Discount: {}. Used count: {}/{}",
                code, voucher.getDiscountAmount(), voucher.getUsedCount(), voucher.getUsageLimit());

        return voucher.getDiscountAmount();
    }

    @Transactional
    public void releaseVoucher(String code) {
        if (code == null || code.trim().isEmpty()) return;

        voucherRepository.findByCode(code).ifPresent(voucher -> {
            if (voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherRepository.save(voucher);
                log.info("Compensating Action: Voucher '{}' released! New used count: {}/{}",
                        code, voucher.getUsedCount(), voucher.getUsageLimit());
            }
        });
    }
}
