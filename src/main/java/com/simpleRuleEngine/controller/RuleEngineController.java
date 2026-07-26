package com.simpleRuleEngine.controller;

import com.simpleRuleEngine.dto.request.RuleExecutionRequest;
import com.simpleRuleEngine.dto.response.RuleExecutionResponse;
import com.simpleRuleEngine.enums.RuleType;
import com.simpleRuleEngine.service.RuleEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rule-engine")
@RequiredArgsConstructor
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;

    @PostMapping("/execute")
    public RuleExecutionResponse execute(@RequestParam RuleType type,
                                          @Valid @RequestBody RuleExecutionRequest request) {
        return ruleEngineService.execute(request.getTransaction(), type);
    }
}
