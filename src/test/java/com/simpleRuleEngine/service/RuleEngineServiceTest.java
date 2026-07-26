package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.response.AppliedRuleResponse;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.IdempotencyConflictException;
import com.simpleRuleEngine.model.PaymentTransaction;
import com.simpleRuleEngine.service.strategy.RuleExecutionStrategy;
import com.simpleRuleEngine.service.strategy.RuleExecutionStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private BusinessRuleService businessRuleService;

    @Mock
    private RuleExecutionStrategyRegistry strategyRegistry;

    @Mock
    private RuleExecutionLogService logService;

    @Mock
    private RuleExecutionStrategy strategy;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    private static final String FINGERPRINT = "test-fingerprint";

    private PaymentTransaction sampleTransaction() {
        return PaymentTransaction.builder()
                .transactionReference("TXN-001")
                .amount(BigDecimal.valueOf(500))
                .currency("USD")
                .direction("INBOUND")
                .status("PENDING")
                .build();
    }

    private RuleExecutionResponse sampleResponse(PaymentTransaction transaction) {
        return RuleExecutionResponse.builder()
                .transaction(transaction)
                .appliedRuleCount(2)
                .appliedRules(List.of(
                        AppliedRuleResponse.builder().ruleCode("ENRICH_DESCRIPTION").name("Enrich Description").priority(1).build(),
                        AppliedRuleResponse.builder().ruleCode("ENRICH_STATUS").name("Enrich Status").priority(2).build()))
                .build();
    }

    @Test
    void execute_loadsEnrichmentRulesAscAndSavesSuccessLog() {
        PaymentTransaction transaction = sampleTransaction();
        List<BusinessRule> rules = List.of(BusinessRule.builder().build());
        RuleExecutionResponse response = sampleResponse(transaction);

        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(rules);
        when(strategyRegistry.getStrategy(RuleType.ENRICHMENT)).thenReturn(strategy);
        when(strategy.execute(transaction, rules)).thenReturn(response);

        RuleExecutionResponse result = ruleEngineService.execute(transaction, RuleType.ENRICHMENT, null);

        assertThat(result).isEqualTo(response);
        verify(businessRuleService).findEnabledByTypeAsc(RuleType.ENRICHMENT);
        verify(logService).saveSuccess(isNull(), any(PaymentTransaction.class), eq(response), eq(RuleType.ENRICHMENT), isNull());
        verify(logService, never()).saveFailure(any(), any(), any(), any(), any());
    }

    @Test
    void execute_loadsRoutingRulesDescAndSavesSuccessLog() {
        PaymentTransaction transaction = sampleTransaction();
        List<BusinessRule> rules = List.of(BusinessRule.builder().build());
        RuleExecutionResponse response = sampleResponse(transaction);

        when(businessRuleService.findEnabledByTypeDesc(RuleType.ROUTING)).thenReturn(rules);
        when(strategyRegistry.getStrategy(RuleType.ROUTING)).thenReturn(strategy);
        when(strategy.execute(transaction, rules)).thenReturn(response);

        RuleExecutionResponse result = ruleEngineService.execute(transaction, RuleType.ROUTING, null);

        assertThat(result).isEqualTo(response);
        verify(businessRuleService).findEnabledByTypeDesc(RuleType.ROUTING);
        verify(logService).saveSuccess(isNull(), any(PaymentTransaction.class), eq(response), eq(RuleType.ROUTING), isNull());
    }

    @Test
    void execute_idempotencyKey_returnsCachedResponse_withoutCallingStrategy() {
        PaymentTransaction transaction = sampleTransaction();
        RuleExecutionResponse cached = sampleResponse(transaction);

        when(logService.computeFingerprint(eq(RuleType.ENRICHMENT), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency("KEY-123", FINGERPRINT)).thenReturn(Optional.of(cached));

        RuleExecutionResponse result = ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "KEY-123");

        assertThat(result).isEqualTo(cached);
        verifyNoInteractions(businessRuleService, strategyRegistry, strategy);
        verify(logService, never()).saveSuccess(any(), any(), any(), any(), any());
        verify(logService, never()).saveFailure(any(), any(), any(), any(), any());
    }

    @Test
    void execute_idempotencyKey_preservesAppliedRulesFromCache() {
        PaymentTransaction transaction = sampleTransaction();
        RuleExecutionResponse cached = sampleResponse(transaction);

        when(logService.computeFingerprint(any(), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency(eq("KEY-123"), any())).thenReturn(Optional.of(cached));

        RuleExecutionResponse result = ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "KEY-123");

        assertThat(result.getAppliedRules()).hasSize(2);
        assertThat(result.getAppliedRules()).extracting(AppliedRuleResponse::getRuleCode)
                .containsExactly("ENRICH_DESCRIPTION", "ENRICH_STATUS");
        assertThat(result.getAppliedRuleCount()).isEqualTo(2);
    }

    @Test
    void execute_idempotencyKey_trimsBeforeLookup() {
        PaymentTransaction transaction = sampleTransaction();
        RuleExecutionResponse cached = sampleResponse(transaction);

        when(logService.computeFingerprint(any(), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency(eq("KEY-123"), any())).thenReturn(Optional.of(cached));

        RuleExecutionResponse result = ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "  KEY-123  ");

        assertThat(result).isEqualTo(cached);
        verify(logService).resolveIdempotency(eq("KEY-123"), any());
    }

    @Test
    void execute_idempotencyKey_absent_proceedsNormally() {
        PaymentTransaction transaction = sampleTransaction();
        List<BusinessRule> rules = List.of();
        RuleExecutionResponse response = sampleResponse(transaction);

        when(logService.computeFingerprint(any(), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency(eq("NEW-KEY"), any())).thenReturn(Optional.empty());
        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(rules);
        when(strategyRegistry.getStrategy(RuleType.ENRICHMENT)).thenReturn(strategy);
        when(strategy.execute(transaction, rules)).thenReturn(response);

        ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "NEW-KEY");

        verify(logService).saveSuccess(eq("NEW-KEY"), any(PaymentTransaction.class), eq(response), eq(RuleType.ENRICHMENT), eq(FINGERPRINT));
    }

    @Test
    void execute_blankIdempotencyKey_treatedAsNull() {
        PaymentTransaction transaction = sampleTransaction();
        List<BusinessRule> rules = List.of();
        RuleExecutionResponse response = sampleResponse(transaction);

        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(rules);
        when(strategyRegistry.getStrategy(RuleType.ENRICHMENT)).thenReturn(strategy);
        when(strategy.execute(transaction, rules)).thenReturn(response);

        ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "   ");

        verify(logService, never()).computeFingerprint(any(), any());
        verify(logService, never()).resolveIdempotency(any(), any());
        verify(logService).saveSuccess(isNull(), any(PaymentTransaction.class), eq(response), eq(RuleType.ENRICHMENT), isNull());
    }

    @Test
    void execute_originalTransactionSnapshot_isPreMutation() {
        PaymentTransaction transaction = sampleTransaction();
        List<BusinessRule> rules = List.of();

        doAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setStatus("ENRICHED");
            tx.setDescription("Enriched description");
            return RuleExecutionResponse.builder()
                    .transaction(tx)
                    .appliedRuleCount(1)
                    .appliedRules(List.of())
                    .build();
        }).when(strategy).execute(any(PaymentTransaction.class), eq(rules));

        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(rules);
        when(strategyRegistry.getStrategy(RuleType.ENRICHMENT)).thenReturn(strategy);

        ruleEngineService.execute(transaction, RuleType.ENRICHMENT, null);

        ArgumentCaptor<PaymentTransaction> originalCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(logService).saveSuccess(isNull(), originalCaptor.capture(), any(), eq(RuleType.ENRICHMENT), isNull());

        PaymentTransaction capturedOriginal = originalCaptor.getValue();
        assertThat(capturedOriginal.getStatus()).isEqualTo("PENDING");
        assertThat(capturedOriginal.getDescription()).isNull();
        assertThat(capturedOriginal.getTransactionReference()).isEqualTo("TXN-001");
    }

    @Test
    void execute_failedIdempotencyKey_throwsAndDoesNotExecute() {
        PaymentTransaction transaction = sampleTransaction();

        when(logService.computeFingerprint(any(), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency(eq("FAILED-KEY"), any()))
                .thenThrow(new IdempotencyConflictException(
                        "Execution with idempotency key 'FAILED-KEY' previously failed and cannot be retried"));

        assertThatThrownBy(() -> ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "FAILED-KEY"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("FAILED-KEY");

        verifyNoInteractions(businessRuleService, strategyRegistry, strategy);
        verify(logService, never()).saveSuccess(any(), any(), any(), any(), any());
        verify(logService, never()).saveFailure(any(), any(), any(), any(), any());
    }

    @Test
    void execute_fingerprintMismatch_throwsConflict() {
        PaymentTransaction transaction = sampleTransaction();

        when(logService.computeFingerprint(any(), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency(eq("KEY-REUSED"), eq(FINGERPRINT)))
                .thenThrow(new IdempotencyConflictException(
                        "Idempotency key 'KEY-REUSED' was already used for a different request"));

        assertThatThrownBy(() -> ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "KEY-REUSED"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("KEY-REUSED");

        verifyNoInteractions(businessRuleService, strategyRegistry, strategy);
    }

    @Test
    void execute_strategyThrows_savesFailureLogAndRethrows() {
        PaymentTransaction transaction = sampleTransaction();
        List<BusinessRule> rules = List.of();
        RuntimeException error = new RuntimeException("Strategy failure");

        when(logService.computeFingerprint(any(), any())).thenReturn(FINGERPRINT);
        when(logService.resolveIdempotency(eq("KEY-ERR"), any())).thenReturn(Optional.empty());
        when(businessRuleService.findEnabledByTypeAsc(RuleType.ENRICHMENT)).thenReturn(rules);
        when(strategyRegistry.getStrategy(RuleType.ENRICHMENT)).thenReturn(strategy);
        when(strategy.execute(transaction, rules)).thenThrow(error);

        assertThatThrownBy(() -> ruleEngineService.execute(transaction, RuleType.ENRICHMENT, "KEY-ERR"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Strategy failure");

        ArgumentCaptor<PaymentTransaction> originalCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(logService).saveFailure(eq("KEY-ERR"), originalCaptor.capture(), eq(RuleType.ENRICHMENT), eq("Strategy failure"), eq(FINGERPRINT));
        assertThat(originalCaptor.getValue().getStatus()).isEqualTo("PENDING");
        verify(logService, never()).saveSuccess(any(), any(), any(), any(), any());
    }
}
