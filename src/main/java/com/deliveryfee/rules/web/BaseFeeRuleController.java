package com.deliveryfee.rules.web;

import com.deliveryfee.rules.FeeRuleService;
import com.deliveryfee.rules.dto.BaseFeeRuleRequest;
import com.deliveryfee.rules.dto.BaseFeeRuleResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP-only controller for base fee rule CRUD. */
@RestController
@RequestMapping("/api/v1/rules/base-fees")
@RequiredArgsConstructor
public class BaseFeeRuleController {

    private final FeeRuleService feeRuleService;

    /**
     * @return all base fee rules
     */
    @GetMapping
    public List<BaseFeeRuleResponse> list() {
        return feeRuleService.listBaseFees();
    }

    /**
     * @param request the rule to create
     * @return the created rule
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseFeeRuleResponse create(@Valid @RequestBody BaseFeeRuleRequest request) {
        return feeRuleService.createBaseFee(request);
    }

    /**
     * @param id rule id
     * @param request the new rule state
     * @return the updated rule
     */
    @PutMapping("/{id}")
    public BaseFeeRuleResponse update(
            @PathVariable Long id, @Valid @RequestBody BaseFeeRuleRequest request) {
        return feeRuleService.updateBaseFee(id, request);
    }

    /**
     * @param id rule id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        feeRuleService.deleteBaseFee(id);
    }
}
