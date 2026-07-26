package com.simpleRuleEngine.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    BUSINESS_RULE_NOT_FOUND("BUSINESS_RULE_NOT_FOUND", "Business rule not found"),
    DUPLICATE_RULE_CODE("DUPLICATE_RULE_CODE", "A rule with this ruleCode already exists"),
    DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION", "A data integrity constraint was violated"),
    INVALID_RULE("INVALID_RULE", "Rule configuration is invalid"),
    UNSUPPORTED_RULE_TYPE("UNSUPPORTED_RULE_TYPE", "Unsupported rule type"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed"),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", "A previous execution with this idempotency key has already failed"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred");

    private final String code;
    private final String defaultMessage;
}
