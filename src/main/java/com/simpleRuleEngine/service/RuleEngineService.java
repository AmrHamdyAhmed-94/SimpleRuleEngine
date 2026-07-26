package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;
import com.simpleRuleEngine.service.strategy.RuleExecutionStrategy;
import com.simpleRuleEngine.service.strategy.RuleExecutionStrategyRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final BusinessRuleService businessRuleService;
    private final RuleExecutionStrategyRegistry strategyRegistry;
    private final RuleExecutionLogService logService;

    @Transactional
    public RuleExecutionResponse execute(PaymentTransaction transaction, RuleType ruleType, String idempotencyKey) {
        String normalizedKey = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null;

        PaymentTransaction originalSnapshot = snapshotOf(transaction);

        if (normalizedKey != null) {
            String fingerprint = logService.computeFingerprint(ruleType, originalSnapshot);
            Optional<RuleExecutionResponse> cached = logService.resolveIdempotency(normalizedKey, fingerprint);
            if (cached.isPresent()) {
                log.info("Returning idempotent response for key: {}", normalizedKey);
                return cached.get();
            }

            List<BusinessRule> rules = loadRules(ruleType);
            return executeWithLog(transaction, rules, ruleType, normalizedKey, originalSnapshot, fingerprint);
        }

        List<BusinessRule> rules = loadRules(ruleType);
        return executeWithLog(transaction, rules, ruleType, null, originalSnapshot, null);
    }

    private RuleExecutionResponse executeWithLog(PaymentTransaction transaction, List<BusinessRule> rules,
                                                 RuleType ruleType, String normalizedKey,
                                                 PaymentTransaction originalSnapshot, String fingerprint) {
        log.info("Executing {} rules: {} loaded", ruleType, rules.size());
        try {
            RuleExecutionStrategy strategy = strategyRegistry.getStrategy(ruleType);
            RuleExecutionResponse response = strategy.execute(transaction, rules);
            log.info("Execution complete: {} rule(s) applied", response.getAppliedRuleCount());
            logService.saveSuccess(normalizedKey, originalSnapshot, response, ruleType, fingerprint);
            return response;
        } catch (Exception ex) {
            logService.saveFailure(normalizedKey, originalSnapshot, ruleType, ex.getMessage(), fingerprint);
            throw ex;
        }
    }

    private List<BusinessRule> loadRules(RuleType ruleType) {
        return switch (ruleType) {
            case ENRICHMENT -> businessRuleService.findEnabledByTypeAsc(ruleType);
            case ROUTING -> businessRuleService.findEnabledByTypeDesc(ruleType);
        };
    }

    private PaymentTransaction snapshotOf(PaymentTransaction tx) {
        return PaymentTransaction.builder()
                .transactionReference(tx.getTransactionReference())
                .direction(tx.getDirection())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .route(tx.getRoute())
                .description(tx.getDescription())
                .build();
    }
}
