package com.simpleRuleEngine.service.executor;

import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.model.PaymentTransaction;

import java.util.List;

public interface RuleExecutor {

    RuleExecutionResponse execute(PaymentTransaction transaction, List<BusinessRule> rules);
}
