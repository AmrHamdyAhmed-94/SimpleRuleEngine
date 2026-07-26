package com.simpleRuleEngine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    private String transactionReference;
    private String direction;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String route;
    private String description;
}
