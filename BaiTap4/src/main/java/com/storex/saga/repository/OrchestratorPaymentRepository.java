package com.storex.saga.repository;

import com.storex.saga.model.OrchestratorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrchestratorPaymentRepository extends JpaRepository<OrchestratorPayment, Long> {

    Optional<OrchestratorPayment> findByOrderId(Long orderId);
}
