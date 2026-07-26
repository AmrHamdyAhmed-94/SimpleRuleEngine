package com.simpleRuleEngine.entity;

import com.simpleRuleEngine.enums.ExecutionStatus;
import com.simpleRuleEngine.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction_execution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(name = "original_transaction_json", columnDefinition = "text")
    private String originalTransactionJson;

    @Column(name = "final_transaction_json", columnDefinition = "text")
    private String finalTransactionJson;

    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    @Column(name = "request_fingerprint")
    private String requestFingerprint;

    @Column(name = "applied_rule_count")
    private Integer appliedRuleCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
