package com.simpleRuleEngine.engine;

import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.exception.InvalidRuleException;
import com.simpleRuleEngine.model.PaymentTransaction;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RuleConditionEvaluator {

    public boolean evaluate(PaymentTransaction transaction, BusinessRule rule) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(transaction);

        if (!wrapper.isReadableProperty(rule.getConditionField())) {
            throw new InvalidRuleException("Invalid condition field: " + rule.getConditionField());
        }

        Object rawValue = wrapper.getPropertyValue(rule.getConditionField());
        if (rawValue == null) {
            return false;
        }

        ConditionOperator operator = rule.getConditionOperator();
        String expectedValue = rule.getConditionValue();

        if (rawValue instanceof BigDecimal actual) {
            BigDecimal expected;
            try {
                expected = new BigDecimal(expectedValue);
            } catch (NumberFormatException e) {
                throw new InvalidRuleException(
                        "Cannot compare numeric field '" + rule.getConditionField()
                        + "' with non-numeric value: " + expectedValue);
            }
            return applyOperator(actual.compareTo(expected), operator);
        }

        return applyOperator(rawValue.toString().compareTo(expectedValue), operator);
    }

    private boolean applyOperator(int cmp, ConditionOperator operator) {
        return switch (operator) {
            case EQUALS -> cmp == 0;
            case NOT_EQUALS -> cmp != 0;
            case GREATER_THAN -> cmp > 0;
            case GREATER_THAN_OR_EQUALS -> cmp >= 0;
            case LESS_THAN -> cmp < 0;
            case LESS_THAN_OR_EQUALS -> cmp <= 0;
        };
    }
}
