package com.simpleRuleEngine.engine;

import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.exception.InvalidRuleException;
import com.simpleRuleEngine.model.PaymentTransaction;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

@Component
public class RuleActionExecutor {

    public void execute(PaymentTransaction transaction, BusinessRule rule) {
        if (rule.getActionType() == null) {
            throw new InvalidRuleException("Action type must not be null");
        }
        switch (rule.getActionType()) {
            case SET_VALUE -> setValue(transaction, rule);
            default -> throw new InvalidRuleException("Unsupported action type: " + rule.getActionType());
        }
    }

    private void setValue(PaymentTransaction transaction, BusinessRule rule) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(transaction);
        if (!wrapper.isWritableProperty(rule.getActionField())) {
            throw new InvalidRuleException("Invalid action field: " + rule.getActionField());
        }
        try {
            wrapper.setPropertyValue(rule.getActionField(), rule.getActionValue());
        } catch (Exception e) {
            throw new InvalidRuleException(
                    "Cannot set field '" + rule.getActionField()
                    + "' to '" + rule.getActionValue() + "': " + e.getMessage());
        }
    }
}
