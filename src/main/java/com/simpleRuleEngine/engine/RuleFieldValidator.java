package com.simpleRuleEngine.engine;

import com.simpleRuleEngine.exception.InvalidRuleException;
import com.simpleRuleEngine.model.PaymentTransaction;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RuleFieldValidator {

    public void validate(String conditionField, String conditionValue, String actionField, String actionValue) {
        if (conditionField.contains(".")) {
            throw new InvalidRuleException("Nested condition field paths are not supported: " + conditionField);
        }
        if (actionField.contains(".")) {
            throw new InvalidRuleException("Nested action field paths are not supported: " + actionField);
        }

        BeanWrapperImpl wrapper = new BeanWrapperImpl(new PaymentTransaction());

        if (!wrapper.isReadableProperty(conditionField)) {
            throw new InvalidRuleException("Condition field does not exist on PaymentTransaction: " + conditionField);
        }
        if (!wrapper.isWritableProperty(actionField)) {
            throw new InvalidRuleException("Action field does not exist or is not writable on PaymentTransaction: " + actionField);
        }

        Class<?> conditionType = wrapper.getPropertyType(conditionField);
        if (BigDecimal.class.equals(conditionType)) {
            try {
                new BigDecimal(conditionValue);
            } catch (NumberFormatException e) {
                throw new InvalidRuleException(
                        "Condition field '" + conditionField + "' is numeric but conditionValue is not a valid decimal: " + conditionValue);
            }
        }

        Class<?> actionType = wrapper.getPropertyType(actionField);
        if (BigDecimal.class.equals(actionType)) {
            try {
                new BigDecimal(actionValue);
            } catch (NumberFormatException e) {
                throw new InvalidRuleException(
                        "Action field '" + actionField + "' is numeric but actionValue is not a valid decimal: " + actionValue);
            }
        }
    }
}
