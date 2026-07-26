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

class RuleConditionEvaluatorTest {

    private RuleConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleConditionEvaluator();
    }

    private BusinessRule ruleFor(String field, ConditionOperator op, String value) {
        return BusinessRule.builder()
                .ruleCode("TEST_RULE")
                .name("Test Rule")
                .ruleType(RuleType.ENRICHMENT)
                .conditionField(field)
                .conditionOperator(op)
                .conditionValue(value)
                .actionField("description")
                .actionValue("matched")
                .priority(1)
                .build();
    }

    private PaymentTransaction transactionWithCurrency(String currency) {
        return PaymentTransaction.builder()
                .transactionReference("TXN001")
                .direction("INBOUND")
                .amount(BigDecimal.valueOf(500))
                .currency(currency)
                .status("PENDING")
                .build();
    }

    private PaymentTransaction transactionWithAmount(BigDecimal amount) {
        return PaymentTransaction.builder()
                .transactionReference("TXN001")
                .direction("INBOUND")
                .amount(amount)
                .currency("USD")
                .status("PENDING")
                .build();
    }

    @Test
    void stringEquals_matches_whenSameValue() {
        PaymentTransaction tx = transactionWithCurrency("USD");
        BusinessRule rule = ruleFor("currency", ConditionOperator.EQUALS, "USD");
        assertThat(evaluator.evaluate(tx, rule)).isTrue();
    }

    @Test
    void stringEquals_doesNotMatch_whenDifferentValue() {
        PaymentTransaction tx = transactionWithCurrency("EUR");
        BusinessRule rule = ruleFor("currency", ConditionOperator.EQUALS, "USD");
        assertThat(evaluator.evaluate(tx, rule)).isFalse();
    }

    @Test
    void stringNotEquals_matches_whenDifferentValue() {
        PaymentTransaction tx = transactionWithCurrency("EUR");
        BusinessRule rule = ruleFor("currency", ConditionOperator.NOT_EQUALS, "USD");
        assertThat(evaluator.evaluate(tx, rule)).isTrue();
    }

    @Test
    void stringNotEquals_doesNotMatch_whenSameValue() {
        PaymentTransaction tx = transactionWithCurrency("USD");
        BusinessRule rule = ruleFor("currency", ConditionOperator.NOT_EQUALS, "USD");
        assertThat(evaluator.evaluate(tx, rule)).isFalse();
    }

    @Test
    void numericGreaterThan_matches_whenActualIsHigher() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(2000));
        BusinessRule rule = ruleFor("amount", ConditionOperator.GREATER_THAN, "1000");
        assertThat(evaluator.evaluate(tx, rule)).isTrue();
    }

    @Test
    void numericGreaterThan_doesNotMatch_whenActualIsEqual() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(1000));
        BusinessRule rule = ruleFor("amount", ConditionOperator.GREATER_THAN, "1000");
        assertThat(evaluator.evaluate(tx, rule)).isFalse();
    }

    @Test
    void numericGreaterThanOrEquals_matches_whenActualIsEqual() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(1000));
        BusinessRule rule = ruleFor("amount", ConditionOperator.GREATER_THAN_OR_EQUALS, "1000");
        assertThat(evaluator.evaluate(tx, rule)).isTrue();
    }

    @Test
    void numericLessThan_matches_whenActualIsLower() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(500));
        BusinessRule rule = ruleFor("amount", ConditionOperator.LESS_THAN, "1000");
        assertThat(evaluator.evaluate(tx, rule)).isTrue();
    }

    @Test
    void numericLessThan_doesNotMatch_whenActualIsHigher() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(1500));
        BusinessRule rule = ruleFor("amount", ConditionOperator.LESS_THAN, "1000");
        assertThat(evaluator.evaluate(tx, rule)).isFalse();
    }

    @Test
    void numericLessThanOrEquals_matches_whenActualIsEqual() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(1000));
        BusinessRule rule = ruleFor("amount", ConditionOperator.LESS_THAN_OR_EQUALS, "1000");
        assertThat(evaluator.evaluate(tx, rule)).isTrue();
    }

    @Test
    void invalidConditionField_throwsInvalidRuleException() {
        PaymentTransaction tx = transactionWithCurrency("USD");
        BusinessRule rule = ruleFor("nonExistentField", ConditionOperator.EQUALS, "USD");
        assertThatThrownBy(() -> evaluator.evaluate(tx, rule))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("nonExistentField");
    }

    @Test
    void numericFieldWithNonNumericConditionValue_throwsInvalidRuleException() {
        PaymentTransaction tx = transactionWithAmount(BigDecimal.valueOf(1000));
        BusinessRule rule = ruleFor("amount", ConditionOperator.GREATER_THAN, "not-a-number");
        assertThatThrownBy(() -> evaluator.evaluate(tx, rule))
                .isInstanceOf(InvalidRuleException.class)
                .hasMessageContaining("not-a-number");
    }
}
