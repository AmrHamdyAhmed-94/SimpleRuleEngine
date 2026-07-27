package com.simpleRuleEngine.service.executor;

import com.simpleRuleEngine.dto.response.AppliedRuleResponse;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.engine.RuleActionExecutor;
import com.simpleRuleEngine.engine.RuleConditionEvaluator;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.model.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrichmentRuleExecutor implements RuleExecutor {

    private final RuleConditionEvaluator conditionEvaluator;
    private final RuleActionExecutor actionExecutor;

    @Override
    public RuleExecutionResponse execute(PaymentTransaction transaction, List<BusinessRule> rules) {
        List<AppliedRuleResponse> appliedRules = new ArrayList<>();
        for (BusinessRule rule : rules) {
            if (conditionEvaluator.evaluate(transaction, rule)) {
                actionExecutor.execute(transaction, rule);
                appliedRules.add(toAppliedRuleResponse(rule));
            }
        }
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
