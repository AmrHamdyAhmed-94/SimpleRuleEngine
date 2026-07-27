package com.simpleRuleEngine.exception;

import com.simpleRuleEngine.dto.response.ErrorResponse;
import com.simpleRuleEngine.enums.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.beans.TypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.BUSINESS_RULE_NOT_FOUND.getCode())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(DuplicateRuleCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRuleCode(DuplicateRuleCodeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.DUPLICATE_RULE_CODE.getCode())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.DATA_INTEGRITY_VIOLATION.getCode())
                        .message(ErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage())
                        .build());
    }

    @ExceptionHandler(InvalidRuleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRule(InvalidRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.INVALID_RULE.getCode())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(UnsupportedRuleTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedRuleType(UnsupportedRuleTypeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.UNSUPPORTED_RULE_TYPE.getCode())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(TypeMismatchException ex) {
        String paramName = ex.getPropertyName();
        String message = paramName != null
                ? "Invalid value for parameter '" + paramName + "': " + ex.getValue()
                : "Invalid parameter value: " + ex.getValue();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                        .message(message)
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                        .message(ErrorCode.VALIDATION_FAILED.getDefaultMessage())
                        .details(details)
                        .build());
    }
}
