package com.simpleRuleEngine.dto.response;

import com.simpleRuleEngine.enums.ActionType;
import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleResponse {

    private String ruleCode;
    private String name;
    private RuleType ruleType;
    private String conditionField;
    private ConditionOperator conditionOperator;
    private String conditionValue;
    private ActionType actionType;
    private String actionField;
    private String actionValue;
    private Integer priority;
    private Boolean enabled;
}
