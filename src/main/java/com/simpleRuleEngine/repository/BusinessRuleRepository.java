package com.simpleRuleEngine.repository;

import com.simpleRuleEngine.entity.BusinessRule;
import com.simpleRuleEngine.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessRuleRepository extends JpaRepository<BusinessRule, Long> {

    Optional<BusinessRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);

    List<BusinessRule> findByEnabledTrueAndRuleTypeOrderByPriorityAscIdAsc(RuleType ruleType);

    List<BusinessRule> findByEnabledTrueAndRuleTypeOrderByPriorityDescIdAsc(RuleType ruleType);
}
