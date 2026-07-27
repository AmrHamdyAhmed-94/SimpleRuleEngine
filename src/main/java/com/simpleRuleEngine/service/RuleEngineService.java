package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.request.RuleExecutionRequest;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;
import com.simpleRuleEngine.service.executor.EnrichmentRuleExecutor;
import com.simpleRuleEngine.service.executor.RoutingRuleExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final BusinessRuleService businessRuleService;
    private final EnrichmentRuleExecutor enrichmentRuleExecutor;
    private final RoutingRuleExecutor routingRuleExecutor;

    @Transactional
    public RuleExecutionResponse execute(RuleType ruleType, RuleExecutionRequest request) {
        PaymentTransaction transaction = request.getTransaction();
        log.info("Executing {} rules for transaction: {}", ruleType, transaction.getTransactionReference());
        return switch (ruleType) {
            case ENRICHMENT -> enrichmentRuleExecutor.execute(
                    transaction, businessRuleService.findEnabledByTypeAsc(ruleType));
            case ROUTING -> routingRuleExecutor.execute(
                    transaction, businessRuleService.findEnabledByTypeDesc(ruleType));
        };
    }
}
