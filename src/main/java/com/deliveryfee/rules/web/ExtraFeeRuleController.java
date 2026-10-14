package com.deliveryfee.rules.web;

import com.deliveryfee.rules.FeeRuleService;
import com.deliveryfee.rules.dto.ExtraFeeRuleRequest;
import com.deliveryfee.rules.dto.ExtraFeeRuleResponse;
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

/** HTTP-only controller for extra fee rule CRUD. */
@RestController
@RequestMapping("/api/v1/rules/extra-fees")
@RequiredArgsConstructor
public class ExtraFeeRuleController {

    private final FeeRuleService feeRuleService;

    /**
     * @return all extra fee rules
     */
    @GetMapping
    public List<ExtraFeeRuleResponse> list() {
        return feeRuleService.listExtraFees();
    }

    /**
     * @param request the rule to create
     * @return the created rule
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExtraFeeRuleResponse create(@Valid @RequestBody ExtraFeeRuleRequest request) {
        return feeRuleService.createExtraFee(request);
    }

    /**
     * @param id rule id
     * @param request the new rule state
     * @return the updated rule
     */
    @PutMapping("/{id}")
    public ExtraFeeRuleResponse update(
            @PathVariable Long id, @Valid @RequestBody ExtraFeeRuleRequest request) {
        return feeRuleService.updateExtraFee(id, request);
    }

    /**
     * @param id rule id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        feeRuleService.deleteExtraFee(id);
    }
}
