package com.simpleRuleEngine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedRuleResponse {

    private String ruleCode;
    private String name;
    private Integer priority;
}
