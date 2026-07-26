package com.simpleRuleEngine.repository;

import com.simpleRuleEngine.entity.PaymentTransactionExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionExecutionLogRepository extends JpaRepository<PaymentTransactionExecutionLog, Long> {

    Optional<PaymentTransactionExecutionLog> findByIdempotencyKey(String idempotencyKey);
}
