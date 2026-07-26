package com.simpleRuleEngine.service;

import com.simpleRuleEngine.dto.request.BusinessRuleCreateRequest;
import com.simpleRuleEngine.dto.request.BusinessRuleUpdateRequest;
import com.simpleRuleEngine.dto.response.BusinessRuleResponse;
import com.simpleRuleEngine.engine.RuleFieldValidator;
import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.exception.DuplicateRuleCodeException;
import com.simpleRuleEngine.exception.ResourceNotFoundException;
import com.simpleRuleEngine.mapper.BusinessRuleMapper;
import com.simpleRuleEngine.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessRuleService {

    private final BusinessRuleRepository repository;
    private final BusinessRuleMapper mapper;
    private final RuleFieldValidator ruleFieldValidator;

    @Transactional
    public BusinessRuleResponse create(BusinessRuleCreateRequest request) {
        String normalizedCode = request.getRuleCode().trim().toUpperCase();
        if (repository.existsByRuleCode(normalizedCode)) {
            throw new DuplicateRuleCodeException("Rule with ruleCode '" + normalizedCode + "' already exists");
        }
        ruleFieldValidator.validate(
                request.getConditionField(), request.getConditionValue(),
                request.getActionField(), request.getActionValue());
        BusinessRule rule = mapper.toEntity(request);
        rule.setRuleCode(normalizedCode);
        return mapper.toResponse(repository.save(rule));
    }

    @Transactional
    public BusinessRuleResponse update(String ruleCode, BusinessRuleUpdateRequest request) {
        BusinessRule existing = findByRuleCodeOrThrow(ruleCode.trim().toUpperCase());
        ruleFieldValidator.validate(
                request.getConditionField(), request.getConditionValue(),
                request.getActionField(), request.getActionValue());
        mapper.updateEntity(request, existing);
        return mapper.toResponse(repository.save(existing));
    }

    @Transactional(readOnly = true)
    public BusinessRuleResponse getByRuleCode(String ruleCode) {
        return mapper.toResponse(findByRuleCodeOrThrow(ruleCode.trim().toUpperCase()));
    }

    @Transactional(readOnly = true)
    public List<BusinessRuleResponse> getAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String ruleCode) {
        BusinessRule existing = findByRuleCodeOrThrow(ruleCode.trim().toUpperCase());
        repository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<BusinessRule> findEnabledByTypeAsc(RuleType ruleType) {
        return repository.findByEnabledTrueAndRuleTypeOrderByPriorityAscIdAsc(ruleType);
    }

    @Transactional(readOnly = true)
    public List<BusinessRule> findEnabledByTypeDesc(RuleType ruleType) {
        return repository.findByEnabledTrueAndRuleTypeOrderByPriorityDescIdAsc(ruleType);
    }

    private BusinessRule findByRuleCodeOrThrow(String ruleCode) {
        return repository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessRule not found with ruleCode: " + ruleCode));
    }
}
