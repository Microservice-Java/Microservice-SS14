package com.storex.saga.repository;

import com.storex.saga.model.ShipmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentRecord, Long> {

    Optional<ShipmentRecord> findByOrderId(Long orderId);
}
