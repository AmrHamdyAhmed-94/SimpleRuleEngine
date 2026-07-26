package com.simpleRuleEngine.engine;

import com.simpleRuleEngine.entity.BusinessRule;
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
    void execute_updatesStringField() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = ruleWithAction("route", "PRIORITY_LANE");

        executor.execute(tx, rule);

        assertThat(tx.getRoute()).isEqualTo("PRIORITY_LANE");
    }

    @Test
    void execute_updatesStatusField() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = ruleWithAction("status", "APPROVED");

        executor.execute(tx, rule);

        assertThat(tx.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void execute_updatesBigDecimalField() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = ruleWithAction("amount", "9999.99");

        executor.execute(tx, rule);

        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("9999.99"));
    }

    @Test
    void execute_updatesDescriptionField() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = ruleWithAction("description", "Enriched by rule");

        executor.execute(tx, rule);

        assertThat(tx.getDescription()).isEqualTo("Enriched by rule");
    }

    @Test
    void execute_invalidActionField_throwsInvalidRuleException() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = ruleWithAction("nonExistentField", "someValue");

        assertThatThrownBy(() -> executor.execute(tx, rule))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("nonExistentField");
    }
}
