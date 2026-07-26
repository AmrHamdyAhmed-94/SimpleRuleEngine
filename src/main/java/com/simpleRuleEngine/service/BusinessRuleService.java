package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.request.BusinessRuleCreateRequest;
import com.simpleRuleEngine.dto.request.BusinessRuleUpdateRequest;
import com.simpleRuleEngine.dto.response.BusinessRuleResponse;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.ResourceNotFoundException;
import com.simpleRuleEngine.mapper.BusinessRuleMapper;
import com.simpleRuleEngine.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessRuleService {

    private final BusinessRuleRepository repository;
    private final BusinessRuleMapper mapper;

    public BusinessRuleResponse create(BusinessRuleCreateRequest request) {
        BusinessRule rule = mapper.toEntity(request);
        rule.setRuleCode(request.getRuleCode().trim().toUpperCase());
        return mapper.toResponse(repository.save(rule));
    }

    public BusinessRuleResponse update(String ruleCode, BusinessRuleUpdateRequest request) {
        BusinessRule existing = findByRuleCodeOrThrow(ruleCode.trim().toUpperCase());
        mapper.updateEntity(request, existing);
        return mapper.toResponse(repository.save(existing));
    }

    public BusinessRuleResponse getByRuleCode(String ruleCode) {
        return mapper.toResponse(findByRuleCodeOrThrow(ruleCode.trim().toUpperCase()));
    }

    public List<BusinessRuleResponse> getAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public void delete(String ruleCode) {
        BusinessRule existing = findByRuleCodeOrThrow(ruleCode.trim().toUpperCase());
        repository.delete(existing);
    }

    public List<BusinessRule> findEnabledByTypeAsc(RuleType ruleType) {
        return repository.findByEnabledTrueAndRuleTypeOrderByPriorityAsc(ruleType);
    }

    public List<BusinessRule> findEnabledByTypeDesc(RuleType ruleType) {
        return repository.findByEnabledTrueAndRuleTypeOrderByPriorityDesc(ruleType);
    }

    private BusinessRule findByRuleCodeOrThrow(String ruleCode) {
        return repository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessRule not found with ruleCode: " + ruleCode));
    }
}
