package com.simpleRuleEngine.engine;

import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.ActionType;
import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.InvalidRuleException;
import com.simpleRuleEngine.model.PaymentTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleActionExecutorTest {

    private RuleActionExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new RuleActionExecutor();
    }

    private BusinessRule ruleWithAction(String actionField, String actionValue) {
        return BusinessRule.builder()
                .ruleCode("TEST_RULE")
                .name("Test Rule")
                .ruleType(RuleType.ENRICHMENT)
                .conditionField("currency")
                .conditionOperator(ConditionOperator.EQUALS)
                .conditionValue("USD")
                .actionType(ActionType.SET_VALUE)
                .actionField(actionField)
                .actionValue(actionValue)
                .priority(1)
                .build();
    }

    private PaymentTransaction baseTransaction() {
        return PaymentTransaction.builder()
                .transactionReference("TXN001")
                .direction("INBOUND")
                .amount(BigDecimal.valueOf(500))
                .currency("USD")
                .status("PENDING")
                .build();
    }

    @Test
    void setValue_updatesStringField() {
        PaymentTransaction tx = baseTransaction();
        executor.execute(tx, ruleWithAction("route", "PRIORITY_LANE"));
        assertThat(tx.getRoute()).isEqualTo("PRIORITY_LANE");
    }

    @Test
    void setValue_updatesStatusField() {
        PaymentTransaction tx = baseTransaction();
        executor.execute(tx, ruleWithAction("status", "APPROVED"));
        assertThat(tx.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void setValue_updatesBigDecimalField() {
        PaymentTransaction tx = baseTransaction();
        executor.execute(tx, ruleWithAction("amount", "9999.99"));
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("9999.99"));
    }

    @Test
    void setValue_updatesDescriptionField() {
        PaymentTransaction tx = baseTransaction();
        executor.execute(tx, ruleWithAction("description", "Enriched by rule"));
        assertThat(tx.getDescription()).isEqualTo("Enriched by rule");
    }

    @Test
    void setValue_invalidActionField_throwsInvalidRuleException() {
        PaymentTransaction tx = baseTransaction();
        assertThatThrownBy(() -> executor.execute(tx, ruleWithAction("nonExistentField", "someValue")))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("nonExistentField");
    }

    @Test
    void nullActionType_throwsInvalidRuleException() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = BusinessRule.builder()
                .ruleCode("NULL_TYPE")
                .name("Null Type")
                .ruleType(RuleType.ENRICHMENT)
                .conditionField("currency")
                .conditionOperator(ConditionOperator.EQUALS)
                .conditionValue("USD")
                .actionType(null)
                .actionField("status")
                .actionValue("X")
                .priority(1)
                .build();
        assertThatThrownBy(() -> executor.execute(tx, rule))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("must not be null");
    }
}
