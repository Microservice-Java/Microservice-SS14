package com.storex.saga.repository;

import com.storex.saga.model.OrchestratorShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrchestratorShipmentRepository extends JpaRepository<OrchestratorShipment, Long> {

    Optional<OrchestratorShipment> findByOrderId(Long orderId);
}
