package com.simpleRuleEngine.dto.request;

import com.simpleRuleEngine.model.PaymentTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RuleExecutionRequest {

    @NotNull
    @Valid
    private PaymentTransaction transaction;
}
