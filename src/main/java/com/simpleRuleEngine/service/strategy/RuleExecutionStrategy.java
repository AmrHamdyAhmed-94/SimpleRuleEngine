package com.simpleRuleEngine.service.strategy;

import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;

import java.util.List;

public interface RuleExecutionStrategy {

    RuleType getRuleType();

    RuleExecutionResponse execute(PaymentTransaction transaction, List<BusinessRule> rules);
}
