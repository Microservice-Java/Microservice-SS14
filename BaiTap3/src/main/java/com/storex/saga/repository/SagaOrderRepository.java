package com.storex.saga.repository;

import com.storex.saga.model.SagaOrder;
import com.storex.saga.model.SagaOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SagaOrderRepository extends JpaRepository<SagaOrder, Long> {

    List<SagaOrder> findByStatusAndUpdatedAtBefore(SagaOrderStatus status, LocalDateTime threshold);
}
