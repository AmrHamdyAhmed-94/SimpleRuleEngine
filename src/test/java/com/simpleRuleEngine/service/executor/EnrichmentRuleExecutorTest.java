package com.simpleRuleEngine.service.executor;

import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.engine.RuleActionExecutor;
import com.simpleRuleEngine.engine.RuleConditionEvaluator;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.ActionType;
import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrichmentRuleExecutorTest {

    @Mock
    private RuleConditionEvaluator conditionEvaluator;

    @Mock
    private RuleActionExecutor actionExecutor;

    @InjectMocks
    private EnrichmentRuleExecutor executor;

    private PaymentTransaction baseTransaction() {
        return PaymentTransaction.builder()
                .transactionReference("TXN001")
                .direction("INBOUND")
                .amount(BigDecimal.valueOf(500))
                .currency("USD")
                .status("PENDING")
                .build();
    }

    private BusinessRule rule(String code, int priority) {
        return BusinessRule.builder()
                .ruleCode(code)
                .name("Rule " + code)
                .ruleType(RuleType.ENRICHMENT)
                .conditionField("currency")
                .conditionOperator(ConditionOperator.EQUALS)
                .conditionValue("USD")
                .actionType(ActionType.SET_VALUE)
                .actionField("status")
                .actionValue("ENRICHED")
                .priority(priority)
                .build();
    }

    @Test
    void execute_appliesAllMatchingRulesInOrder() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule1 = rule("ENRICH_001", 1);
        BusinessRule rule2 = rule("ENRICH_002", 2);
        BusinessRule rule3 = rule("ENRICH_003", 3);

        when(conditionEvaluator.evaluate(tx, rule1)).thenReturn(true);
        when(conditionEvaluator.evaluate(tx, rule2)).thenReturn(true);
        when(conditionEvaluator.evaluate(tx, rule3)).thenReturn(true);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule1, rule2, rule3));

        assertThat(response.getAppliedRuleCount()).isEqualTo(3);
        assertThat(response.getAppliedRules()).extracting("ruleCode")
                .containsExactly("ENRICH_001", "ENRICH_002", "ENRICH_003");
        verify(actionExecutor).execute(tx, rule1);
        verify(actionExecutor).execute(tx, rule2);
        verify(actionExecutor).execute(tx, rule3);
    }

    @Test
    void execute_skipsNonMatchingRules() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule1 = rule("ENRICH_001", 1);
        BusinessRule rule2 = rule("ENRICH_002", 2);

        when(conditionEvaluator.evaluate(tx, rule1)).thenReturn(true);
        when(conditionEvaluator.evaluate(tx, rule2)).thenReturn(false);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule1, rule2));

        assertThat(response.getAppliedRuleCount()).isEqualTo(1);
        assertThat(response.getAppliedRules()).extracting("ruleCode").containsExactly("ENRICH_001");
        verify(actionExecutor).execute(tx, rule1);
        verify(actionExecutor, never()).execute(tx, rule2);
    }

    @Test
    void execute_returnsZeroApplied_whenNoRulesMatch() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule1 = rule("ENRICH_001", 1);

        when(conditionEvaluator.evaluate(tx, rule1)).thenReturn(false);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule1));

        assertThat(response.getAppliedRuleCount()).isEqualTo(0);
        assertThat(response.getAppliedRules()).isEmpty();
        assertThat(response.getTransaction()).isEqualTo(tx);
        verify(actionExecutor, never()).execute(any(), any());
    }

    @Test
    void execute_returnsZeroApplied_whenRuleListIsEmpty() {
        PaymentTransaction tx = baseTransaction();

        RuleExecutionResponse response = executor.execute(tx, List.of());

        assertThat(response.getAppliedRuleCount()).isEqualTo(0);
        assertThat(response.getAppliedRules()).isEmpty();
        assertThat(response.getTransaction()).isEqualTo(tx);
    }

    @Test
    void execute_appliedRuleDetailsAreCorrect() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule = rule("ENRICH_STATUS", 15);

        when(conditionEvaluator.evaluate(tx, rule)).thenReturn(true);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule));

        assertThat(response.getAppliedRules()).hasSize(1);
        assertThat(response.getAppliedRules().get(0).getRuleCode()).isEqualTo("ENRICH_STATUS");
        assertThat(response.getAppliedRules().get(0).getName()).isEqualTo("Rule ENRICH_STATUS");
        assertThat(response.getAppliedRules().get(0).getPriority()).isEqualTo(15);
    }
}
