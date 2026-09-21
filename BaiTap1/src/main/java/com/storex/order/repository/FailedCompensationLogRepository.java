package com.storex.order.repository;

import com.storex.order.model.FailedCompensationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedCompensationLogRepository extends JpaRepository<FailedCompensationLog, Long> {
    List<FailedCompensationLog> findByStatus(String status);
}
