package com.simpleRuleEngine.repository;

import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessRuleRepository extends JpaRepository<BusinessRule, Long> {

    Optional<BusinessRule> findByRuleCode(String ruleCode);

    List<BusinessRule> findByEnabledTrueAndRuleTypeOrderByPriorityAsc(RuleType ruleType);

    List<BusinessRule> findByEnabledTrueAndRuleTypeOrderByPriorityDesc(RuleType ruleType);
}
