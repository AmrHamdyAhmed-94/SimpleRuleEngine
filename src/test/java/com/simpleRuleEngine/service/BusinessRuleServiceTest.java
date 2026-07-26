package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.request.BusinessRuleCreateRequest;
import com.simpleRuleEngine.dto.request.BusinessRuleUpdateRequest;
import com.simpleRuleEngine.dto.response.BusinessRuleResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.ResourceNotFoundException;
import com.simpleRuleEngine.mapper.BusinessRuleMapper;
import com.simpleRuleEngine.repository.BusinessRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessRuleServiceTest {

    @Mock
    private BusinessRuleRepository repository;

    @Mock
    private BusinessRuleMapper mapper;

    @InjectMocks
    private BusinessRuleService service;

    private static final String RULE_CODE = "HIGH_VALUE_ROUTE";

    private BusinessRuleCreateRequest validCreateRequest() {
        BusinessRuleCreateRequest req = new BusinessRuleCreateRequest();
        req.setRuleCode(RULE_CODE);
        req.setName("High Value Route");
        req.setRuleType(RuleType.ROUTING);
        req.setConditionField("amount");
        req.setConditionOperator(ConditionOperator.GREATER_THAN);
        req.setConditionValue("1000");
        req.setActionField("route");
        req.setActionValue("PRIORITY");
        req.setPriority(10);
        return req;
    }

    private BusinessRuleUpdateRequest validUpdateRequest() {
        BusinessRuleUpdateRequest req = new BusinessRuleUpdateRequest();
        req.setName("High Value Route Updated");
        req.setRuleType(RuleType.ROUTING);
        req.setConditionField("amount");
        req.setConditionOperator(ConditionOperator.GREATER_THAN_OR_EQUALS);
        req.setConditionValue("2000");
        req.setActionField("route");
        req.setActionValue("VIP");
        req.setPriority(20);
        return req;
    }

    private BusinessRule savedRule() {
        return BusinessRule.builder()
                .id(1L)
                .ruleCode(RULE_CODE)
                .name("High Value Route")
                .ruleType(RuleType.ROUTING)
                .conditionField("amount")
                .conditionOperator(ConditionOperator.GREATER_THAN)
                .conditionValue("1000")
                .actionField("route")
                .actionValue("PRIORITY")
                .priority(10)
                .enabled(true)
                .build();
    }

    private BusinessRuleResponse responseFrom(BusinessRule rule) {
        return BusinessRuleResponse.builder()
                .ruleCode(rule.getRuleCode())
                .name(rule.getName())
                .ruleType(rule.getRuleType())
                .conditionField(rule.getConditionField())
                .conditionOperator(rule.getConditionOperator())
                .conditionValue(rule.getConditionValue())
                .actionField(rule.getActionField())
                .actionValue(rule.getActionValue())
                .priority(rule.getPriority())
                .enabled(rule.getEnabled())
                .build();
    }

    @Test
    void create_normalizesRuleCode() {
        BusinessRuleCreateRequest req = validCreateRequest();
        req.setRuleCode("  high_value_route  ");

        BusinessRule mappedEntity = savedRule();
        mappedEntity.setRuleCode(null);

        when(mapper.toEntity(req)).thenReturn(mappedEntity);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(responseFrom(savedRule()));

        service.create(req);

        assertThat(mappedEntity.getRuleCode()).isEqualTo("HIGH_VALUE_ROUTE");
        verify(repository).save(mappedEntity);
    }

    @Test
    void create_respectsExplicitFalseEnabled() {
        BusinessRuleCreateRequest req = validCreateRequest();
        req.setEnabled(false);

        BusinessRule mappedEntity = savedRule();
        mappedEntity.setEnabled(false);

        when(mapper.toEntity(req)).thenReturn(mappedEntity);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(responseFrom(mappedEntity));

        service.create(req);

        assertThat(mappedEntity.getEnabled()).isFalse();
    }

    @Test
    void getByRuleCode_normalizesPathCode() {
        BusinessRule rule = savedRule();
        when(repository.findByRuleCode(RULE_CODE)).thenReturn(Optional.of(rule));
        when(mapper.toResponse(rule)).thenReturn(responseFrom(rule));

        BusinessRuleResponse result = service.getByRuleCode("  high_value_route  ");

        assertThat(result.getRuleCode()).isEqualTo(RULE_CODE);
        verify(repository).findByRuleCode(RULE_CODE);
    }

    @Test
    void getByRuleCode_throws_whenNotFound() {
        when(repository.findByRuleCode("UNKNOWN_RULE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByRuleCode("UNKNOWN_RULE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN_RULE");
    }

    @Test
    void getAll_returnsAllRules() {
        when(repository.findAll()).thenReturn(List.of(savedRule(), savedRule()));
        when(mapper.toResponse(any())).thenReturn(responseFrom(savedRule()));

        List<BusinessRuleResponse> result = service.getAll();

        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }

    @Test
    void update_normalizesPathCodeAndCallsMapper() {
        BusinessRule existing = savedRule();
        when(repository.findByRuleCode(RULE_CODE)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(responseFrom(existing));

        service.update("  high_value_route  ", validUpdateRequest());

        verify(repository).findByRuleCode(RULE_CODE);
        verify(mapper).updateEntity(any(BusinessRuleUpdateRequest.class), eq(existing));
        verify(repository).save(existing);
    }

    @Test
    void update_throws_whenNotFound() {
        when(repository.findByRuleCode("INBOUND_STATUS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("INBOUND_STATUS", validUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INBOUND_STATUS");
    }

    @Test
    void delete_normalizesPathCodeAndCallsRepository() {
        BusinessRule existing = savedRule();
        when(repository.findByRuleCode(RULE_CODE)).thenReturn(Optional.of(existing));

        service.delete("  high_value_route  ");

        verify(repository).findByRuleCode(RULE_CODE);
        verify(repository).delete(existing);
    }

    @Test
    void delete_throws_whenNotFound() {
        when(repository.findByRuleCode("RULE_001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("RULE_001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RULE_001");
    }

    @Test
    void findEnabledByTypeAsc_delegatesToRepository() {
        when(repository.findByEnabledTrueAndRuleTypeOrderByPriorityAsc(RuleType.ENRICHMENT))
                .thenReturn(List.of(savedRule()));

        List<BusinessRule> result = service.findEnabledByTypeAsc(RuleType.ENRICHMENT);

        assertThat(result).hasSize(1);
        verify(repository).findByEnabledTrueAndRuleTypeOrderByPriorityAsc(RuleType.ENRICHMENT);
    }

    @Test
    void findEnabledByTypeDesc_delegatesToRepository() {
        when(repository.findByEnabledTrueAndRuleTypeOrderByPriorityDesc(RuleType.ROUTING))
                .thenReturn(List.of(savedRule()));

        List<BusinessRule> result = service.findEnabledByTypeDesc(RuleType.ROUTING);

        assertThat(result).hasSize(1);
        verify(repository).findByEnabledTrueAndRuleTypeOrderByPriorityDesc(RuleType.ROUTING);
    }
}
