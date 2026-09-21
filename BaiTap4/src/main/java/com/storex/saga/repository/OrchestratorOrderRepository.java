package com.storex.saga.repository;

import com.storex.saga.model.OrchestratorOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrchestratorOrderRepository extends JpaRepository<OrchestratorOrder, Long> {
}
