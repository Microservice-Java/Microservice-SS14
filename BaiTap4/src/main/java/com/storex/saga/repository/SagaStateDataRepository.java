package com.storex.saga.repository;

import com.storex.saga.model.SagaStateData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SagaStateDataRepository extends JpaRepository<SagaStateData, Long> {

    Optional<SagaStateData> findByOrderId(Long orderId);
}
