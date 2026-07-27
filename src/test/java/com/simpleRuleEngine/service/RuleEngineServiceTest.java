package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.request.RuleExecutionRequest;
import com.simpleRuleEngine.dto.response.AppliedRuleResponse;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.model.PaymentTransaction;
import com.simpleRuleEngine.service.executor.EnrichmentRuleExecutor;
import com.simpleRuleEngine.service.executor.RoutingRuleExecutor;
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
class RuleEngineServiceTest {

    @Mock
    private BusinessRuleService businessRuleService;

    @Mock
    private EnrichmentRuleExecutor enrichmentRuleExecutor;

    @Mock
    private RoutingRuleExecutor routingRuleExecutor;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    private PaymentTransaction sampleTransaction() {
        return PaymentTransaction.builder()
                .transactionReference("TXN-001")
                .amount(BigDecimal.valueOf(500))
                .currency("USD")
                .direction("INBOUND")
                .status("PENDING")
                .build();
    }

    private RuleExecutionRequest requestOf(PaymentTransaction tx) {
        RuleExecutionRequest req = new RuleExecutionRequest();
        req.setTransaction(tx);
        return req;
    }

    private RuleExecutionResponse sampleResponse(PaymentTransaction tx, String ruleCode) {
        return RuleExecutionResponse.builder()
                .transaction(tx)
                .appliedRuleCount(1)
                .appliedRules(List.of(
                        AppliedRuleResponse.builder().ruleCode(ruleCode).name(ruleCode).priority(10).build()))
                .build();
    }

    @Test
    void execute_enrichment_loadsRulesAscAndDelegatesToEnrichmentExecutor() {
        PaymentTransaction tx = sampleTransaction();
        List<BusinessRule> rules = List.of(BusinessRule.builder().build());
        RuleExecutionResponse response = sampleResponse(tx, "ENRICH_STATUS");

        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(rules);
        when(enrichmentRuleExecutor.execute(tx, rules)).thenReturn(response);

        RuleExecutionResponse result = ruleEngineService.execute(RuleType.ENRICHMENT, requestOf(tx));

        assertThat(result).isEqualTo(response);
        verify(businessRuleService).findEnabledByTypeAsc(RuleType.ENRICHMENT);
        verify(enrichmentRuleExecutor).execute(tx, rules);
        verifyNoInteractions(routingRuleExecutor);
    }

    @Test
    void execute_routing_loadsRulesDescAndDelegatesToRoutingExecutor() {
        PaymentTransaction tx = sampleTransaction();
        List<BusinessRule> rules = List.of(BusinessRule.builder().build());
        RuleExecutionResponse response = sampleResponse(tx, "ROUTE_USD");

        when(businessRuleService.findEnabledByTypeDesc(RuleType.ROUTING)).thenReturn(rules);
        when(routingRuleExecutor.execute(tx, rules)).thenReturn(response);

        RuleExecutionResponse result = ruleEngineService.execute(RuleType.ROUTING, requestOf(tx));

        assertThat(result).isEqualTo(response);
        verify(businessRuleService).findEnabledByTypeDesc(RuleType.ROUTING);
        verify(routingRuleExecutor).execute(tx, rules);
        verifyNoInteractions(enrichmentRuleExecutor);
    }

    @Test
    void execute_enrichment_neverCallsRoutingExecutor() {
        PaymentTransaction tx = sampleTransaction();
        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(List.of());
        when(enrichmentRuleExecutor.execute(any(), any())).thenReturn(sampleResponse(tx, "R"));

        ruleEngineService.execute(RuleType.ENRICHMENT, requestOf(tx));

        verify(businessRuleService, never()).findEnabledByTypeDesc(any());
        verifyNoInteractions(routingRuleExecutor);
    }

    @Test
    void execute_routing_neverCallsEnrichmentExecutor() {
        PaymentTransaction tx = sampleTransaction();
        when(businessRuleService.findEnabledByTypeDesc(RuleType.ROUTING)).thenReturn(List.of());
        when(routingRuleExecutor.execute(any(), any())).thenReturn(sampleResponse(tx, "R"));

        ruleEngineService.execute(RuleType.ROUTING, requestOf(tx));

        verify(businessRuleService, never()).findEnabledByTypeAsc(any());
        verifyNoInteractions(enrichmentRuleExecutor);
    }

    @Test
    void execute_returnsExecutorResponseDirectly() {
        PaymentTransaction tx = sampleTransaction();
        RuleExecutionResponse expected = RuleExecutionResponse.builder()
                .transaction(tx).appliedRuleCount(0).appliedRules(List.of()).build();

        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(List.of());
        when(enrichmentRuleExecutor.execute(tx, List.of())).thenReturn(expected);

        RuleExecutionResponse result = ruleEngineService.execute(RuleType.ENRICHMENT, requestOf(tx));

        assertThat(result.getAppliedRuleCount()).isEqualTo(0);
        assertThat(result.getTransaction()).isEqualTo(tx);
    }
}
