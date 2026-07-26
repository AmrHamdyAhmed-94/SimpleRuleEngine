package com.simpleRuleEngine.strategy;

import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.UnsupportedRuleTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RuleExecutionStrategyRegistry {

    private final Map<RuleType, RuleExecutionStrategy> strategies;

    public RuleExecutionStrategyRegistry(List<RuleExecutionStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(RuleExecutionStrategy::getRuleType, Function.identity()));
    }

    public RuleExecutionStrategy getStrategy(RuleType ruleType) {
        RuleExecutionStrategy strategy = strategies.get(ruleType);
        if (strategy == null) {
            throw new UnsupportedRuleTypeException("No strategy registered for rule type: " + ruleType);
        }
        return strategy;
    }
}
