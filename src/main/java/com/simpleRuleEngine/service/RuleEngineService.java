package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;
import com.simpleRuleEngine.strategy.RuleExecutionStrategy;
import com.simpleRuleEngine.strategy.RuleExecutionStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final BusinessRuleService businessRuleService;
    private final RuleExecutionStrategyRegistry strategyRegistry;

    public RuleExecutionResponse execute(PaymentTransaction transaction, RuleType ruleType) {
        List<BusinessRule> rules = switch (ruleType) {
            case ENRICHMENT -> businessRuleService.findEnabledByTypeAsc(ruleType);
            case ROUTING -> businessRuleService.findEnabledByTypeDesc(ruleType);
        };

        RuleExecutionStrategy strategy = strategyRegistry.getStrategy(ruleType);
        return strategy.execute(transaction, rules);
    }
}
