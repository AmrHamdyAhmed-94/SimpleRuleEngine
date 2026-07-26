package com.simpleRuleEngine.dto.request;

import com.simpleRuleEngine.enums.ConditionOperator;
import com.simpleRuleEngine.enums.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BusinessRuleCreateRequest {

    @NotBlank
    private String ruleCode;

    @NotBlank
    private String name;

    @NotNull
    private RuleType ruleType;

    @NotBlank
    private String conditionField;

    @NotNull
    private ConditionOperator conditionOperator;

    @NotBlank
    private String conditionValue;

    @NotBlank
    private String actionField;

    @NotBlank
    private String actionValue;

    @NotNull
    private Integer priority;

    private Boolean enabled;
}
