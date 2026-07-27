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
class RoutingRuleExecutorTest {

    @Mock
    private RuleConditionEvaluator conditionEvaluator;

    @Mock
    private RuleActionExecutor actionExecutor;

    @InjectMocks
    private RoutingRuleExecutor executor;

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
                .ruleType(RuleType.ROUTING)
                .conditionField("currency")
                .conditionOperator(ConditionOperator.EQUALS)
                .conditionValue("USD")
                .actionType(ActionType.SET_VALUE)
                .actionField("route")
                .actionValue("LANE_A")
                .priority(priority)
                .build();
    }

    @Test
    void execute_appliesOnlyFirstMatchingRule() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule1 = rule("ROUTE_001", 10);
        BusinessRule rule2 = rule("ROUTE_002", 5);

        when(conditionEvaluator.evaluate(tx, rule1)).thenReturn(true);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule1, rule2));

        assertThat(response.getAppliedRuleCount()).isEqualTo(1);
        assertThat(response.getAppliedRules()).extracting("ruleCode").containsExactly("ROUTE_001");
        verify(actionExecutor).execute(tx, rule1);
        verify(conditionEvaluator, never()).evaluate(tx, rule2);
        verify(actionExecutor, never()).execute(tx, rule2);
    }

    @Test
    void execute_skipsFirstNonMatchingRule_andAppliesSecond() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule1 = rule("ROUTE_001", 10);
        BusinessRule rule2 = rule("ROUTE_002", 5);

        when(conditionEvaluator.evaluate(tx, rule1)).thenReturn(false);
        when(conditionEvaluator.evaluate(tx, rule2)).thenReturn(true);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule1, rule2));

        assertThat(response.getAppliedRuleCount()).isEqualTo(1);
        assertThat(response.getAppliedRules()).extracting("ruleCode").containsExactly("ROUTE_002");
        verify(actionExecutor, never()).execute(tx, rule1);
        verify(actionExecutor).execute(tx, rule2);
    }

    @Test
    void execute_returnsZeroApplied_whenNoRulesMatch() {
        PaymentTransaction tx = baseTransaction();
        BusinessRule rule1 = rule("ROUTE_001", 10);

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
        BusinessRule rule = rule("ROUTE_HIGH_VALUE", 100);

        when(conditionEvaluator.evaluate(tx, rule)).thenReturn(true);

        RuleExecutionResponse response = executor.execute(tx, List.of(rule));

        assertThat(response.getAppliedRules()).hasSize(1);
        assertThat(response.getAppliedRules().get(0).getRuleCode()).isEqualTo("ROUTE_HIGH_VALUE");
        assertThat(response.getAppliedRules().get(0).getPriority()).isEqualTo(100);
    }
}
