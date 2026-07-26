package com.simpleRuleEngine.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    BUSINESS_RULE_NOT_FOUND("BUSINESS_RULE_NOT_FOUND", "Business rule not found"),
    DUPLICATE_RULE_CODE("DUPLICATE_RULE_CODE", "A rule with this ruleCode already exists"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred");

    private final String code;
    private final String defaultMessage;
}
