package com.simpleRuleEngine.entity;

import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", unique = true, nullable = false, updatable = false)
    private String ruleCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    @Column(name = "condition_field", nullable = false)
    private String conditionField;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_operator", nullable = false)
    private ConditionOperator conditionOperator;

    @Column(name = "condition_value", nullable = false)
    private String conditionValue;

    @Column(name = "action_field", nullable = false)
    private String actionField;

    @Column(name = "action_value", nullable = false)
    private String actionValue;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Boolean enabled;
}
