package com.simpleRuleEngine.dto.response;

import com.simpleRuleEngine.model.PaymentTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecutionResponse {

    private PaymentTransaction transaction;
    private Integer appliedRuleCount;
    private List<AppliedRuleResponse> appliedRules;
}
