package com.simpleRuleEngine.strategy;

import com.simpleRuleEngine.dto.response.AppliedRuleResponse;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.engine.RuleActionExecutor;
import com.simpleRuleEngine.engine.RuleConditionEvaluator;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoutingRuleStrategy implements RuleExecutionStrategy {

    private final RuleConditionEvaluator conditionEvaluator;
    private final RuleActionExecutor actionExecutor;

    @Override
    public RuleType getRuleType() {
        return RuleType.ROUTING;
    }

    @Override
    public RuleExecutionResponse execute(PaymentTransaction transaction, List<BusinessRule> rules) {
        return rules.stream()
                .filter(rule -> conditionEvaluator.evaluate(transaction, rule))
                .findFirst()
                .map(rule -> {
                    actionExecutor.execute(transaction, rule);
                    return RuleExecutionResponse.builder()
                            .transaction(transaction)
                            .appliedRuleCount(1)
                            .appliedRules(List.of(toAppliedRuleResponse(rule)))
                            .build();
                })
                .orElse(RuleExecutionResponse.builder()
                        .transaction(transaction)
                        .appliedRuleCount(0)
                        .appliedRules(List.of())
                        .build());
    }

    private AppliedRuleResponse toAppliedRuleResponse(BusinessRule rule) {
        return AppliedRuleResponse.builder()
                .ruleCode(rule.getRuleCode())
                .name(rule.getName())
                .priority(rule.getPriority())
                .build();
    }
}
