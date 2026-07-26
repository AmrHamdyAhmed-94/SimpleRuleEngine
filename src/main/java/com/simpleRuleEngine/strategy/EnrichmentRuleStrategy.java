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
public class EnrichmentRuleStrategy implements RuleExecutionStrategy {

    private final RuleConditionEvaluator conditionEvaluator;
    private final RuleActionExecutor actionExecutor;

    @Override
    public RuleType getRuleType() {
        return RuleType.ENRICHMENT;
    }

    @Override
    public RuleExecutionResponse execute(PaymentTransaction transaction, List<BusinessRule> rules) {
        List<BusinessRule> matchedRules = rules.stream()
                .filter(rule -> conditionEvaluator.evaluate(transaction, rule))
                .toList();

        matchedRules.forEach(rule -> actionExecutor.execute(transaction, rule));

        List<AppliedRuleResponse> appliedRules = matchedRules.stream()
                .map(this::toAppliedRuleResponse)
                .toList();

        return RuleExecutionResponse.builder()
                .transaction(transaction)
                .appliedRuleCount(appliedRules.size())
                .appliedRules(appliedRules)
                .build();
    }

    private AppliedRuleResponse toAppliedRuleResponse(BusinessRule rule) {
        return AppliedRuleResponse.builder()
                .ruleCode(rule.getRuleCode())
                .name(rule.getName())
                .priority(rule.getPriority())
                .build();
    }
}
