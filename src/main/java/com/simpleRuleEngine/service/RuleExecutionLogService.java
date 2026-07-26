package com.simpleRuleEngine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.PaymentTransactionExecutionLog;
import com.simpleRuleEngine.enums.ExecutionStatus;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.IdempotencyConflictException;
import com.simpleRuleEngine.model.PaymentTransaction;
import com.simpleRuleEngine.repository.PaymentTransactionExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutionLogService {

    private final PaymentTransactionExecutionLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public Optional<RuleExecutionResponse> resolveIdempotency(String idempotencyKey) {
        return logRepository.findByIdempotencyKey(idempotencyKey)
                .map(entry -> {
                    if (entry.getStatus() == ExecutionStatus.FAILED) {
                        throw new IdempotencyConflictException(
                                "Execution with idempotency key '" + idempotencyKey + "' previously failed and cannot be retried");
                    }
                    return Optional.of(toResponse(entry));
                })
                .orElse(Optional.empty());
    }

    @Transactional
    public void saveSuccess(String idempotencyKey, PaymentTransaction originalSnapshot,
                            RuleExecutionResponse response, RuleType ruleType) {
        PaymentTransactionExecutionLog entry = PaymentTransactionExecutionLog.builder()
                .idempotencyKey(idempotencyKey)
                .transactionReference(response.getTransaction().getTransactionReference())
                .ruleType(ruleType)
                .originalTransactionJson(toJson(originalSnapshot))
                .finalTransactionJson(toJson(response.getTransaction()))
                .responseJson(toJson(response))
                .appliedRuleCount(response.getAppliedRuleCount())
                .status(ExecutionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(String idempotencyKey, PaymentTransaction originalSnapshot,
                            RuleType ruleType, String errorMessage) {
        PaymentTransactionExecutionLog entry = PaymentTransactionExecutionLog.builder()
                .idempotencyKey(idempotencyKey)
                .transactionReference(originalSnapshot.getTransactionReference())
                .ruleType(ruleType)
                .originalTransactionJson(toJson(originalSnapshot))
                .status(ExecutionStatus.FAILED)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(entry);
    }

    private RuleExecutionResponse toResponse(PaymentTransactionExecutionLog entry) {
        return fromJson(entry.getResponseJson(), RuleExecutionResponse.class);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return null;
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to deserialize from JSON: {}", e.getMessage());
            return null;
        }
    }
}
