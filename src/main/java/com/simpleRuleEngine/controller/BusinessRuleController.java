package com.simpleRuleEngine.controller;

import com.simpleRuleEngine.dto.request.BusinessRuleCreateRequest;
import com.simpleRuleEngine.dto.request.BusinessRuleUpdateRequest;
import com.simpleRuleEngine.dto.response.BusinessRuleResponse;
import com.simpleRuleEngine.service.BusinessRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business-rules")
@RequiredArgsConstructor
public class BusinessRuleController {

    private final BusinessRuleService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessRuleResponse create(@Valid @RequestBody BusinessRuleCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{ruleCode}")
    public BusinessRuleResponse update(@PathVariable String ruleCode,
                                        @Valid @RequestBody BusinessRuleUpdateRequest request) {
        return service.update(ruleCode, request);
    }

    @GetMapping
    public List<BusinessRuleResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{ruleCode}")
    public BusinessRuleResponse getByRuleCode(@PathVariable String ruleCode) {
        return service.getByRuleCode(ruleCode);
    }

    @DeleteMapping("/{ruleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String ruleCode) {
        service.delete(ruleCode);
    }
}
