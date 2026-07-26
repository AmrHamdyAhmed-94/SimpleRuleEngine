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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleExecutionLogService {

    private final PaymentTransactionExecutionLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public String computeFingerprint(RuleType ruleType, PaymentTransaction originalSnapshot) {
        String input = ruleType.name() + "|" + toJson(originalSnapshot);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public Optional<RuleExecutionResponse> resolveIdempotency(String idempotencyKey, String requestFingerprint) {
        return logRepository.findByIdempotencyKey(idempotencyKey)
                .map(entry -> {
                    if (!requestFingerprint.equals(entry.getRequestFingerprint())) {
                        throw new IdempotencyConflictException(
                                "Idempotency key '" + idempotencyKey + "' was already used for a different request");
                    }
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
                            RuleExecutionResponse response, RuleType ruleType, String requestFingerprint) {
        PaymentTransactionExecutionLog entry = PaymentTransactionExecutionLog.builder()
                .idempotencyKey(idempotencyKey)
                .transactionReference(response.getTransaction().getTransactionReference())
                .ruleType(ruleType)
                .originalTransactionJson(toJson(originalSnapshot))
                .finalTransactionJson(toJson(response.getTransaction()))
                .responseJson(toJson(response))
                .requestFingerprint(requestFingerprint)
                .appliedRuleCount(response.getAppliedRuleCount())
                .status(ExecutionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(String idempotencyKey, PaymentTransaction originalSnapshot,
                            RuleType ruleType, String errorMessage, String requestFingerprint) {
        PaymentTransactionExecutionLog entry = PaymentTransactionExecutionLog.builder()
                .idempotencyKey(idempotencyKey)
                .transactionReference(originalSnapshot.getTransactionReference())
                .ruleType(ruleType)
                .originalTransactionJson(toJson(originalSnapshot))
                .requestFingerprint(requestFingerprint)
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
