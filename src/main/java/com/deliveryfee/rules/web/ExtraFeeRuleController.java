package com.deliveryfee.rules.web;

import com.deliveryfee.rules.FeeRuleService;
import com.deliveryfee.rules.dto.ExtraFeeRuleRequest;
import com.deliveryfee.rules.dto.ExtraFeeRuleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Rules — Extra Fees", description = "CRUD for weather extra fee rules")
@RestController
@RequestMapping("/api/v1/rules/extra-fees")
@RequiredArgsConstructor
public class ExtraFeeRuleController {

    // region Dependencies
    private final FeeRuleService feeRuleService;

    // endregion

    // region Public API
    /**
     * @return all extra fee rules
     */
    @Operation(summary = "List all extra fee rules")
    @GetMapping
    public List<ExtraFeeRuleResponse> list() {
        return feeRuleService.listExtraFees();
    }

    /**
     * @param request the rule to create
     * @return the created rule
     */
    @Operation(
            summary = "Create an extra fee rule",
            description =
                    """
                    Adds a weather extra fee (temperature, wind, or phenomenon). Exactly one of
                    numeric bounds or phenomenon category must be present; `fee` is required unless
                    `forbidden` is true. Cars have no seeded extras, so a car rule is a good
                    non-conflicting example.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rule created"),
        @ApiResponse(responseCode = "400", description = "Validation error (bad condition)"),
        @ApiResponse(
                responseCode = "409",
                description = "Conflicting rule for the same condition and vehicle")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Extra fee rule to create",
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            examples = {
                                @ExampleObject(
                                        name = "Phenomenon (rain) for cars",
                                        value =
                                                """
                                                {"conditionType":"PHENOMENON","vehicleType":"CAR","phenomenonCategory":"RAIN","fee":0.50,"validFrom":"2026-01-01T00:00:00Z"}
                                                """),
                                @ExampleObject(
                                        name = "Temperature (-10..0) for cars",
                                        value =
                                                """
                                                {"conditionType":"TEMPERATURE","vehicleType":"CAR","minValue":-10,"minInclusive":true,"maxValue":0,"maxInclusive":true,"fee":0.50,"validFrom":"2026-01-01T00:00:00Z"}
                                                """)
                            }))
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
    @Operation(summary = "Update an extra fee rule", description = "Replaces the rule's fields.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule updated"),
        @ApiResponse(responseCode = "404", description = "Unknown rule id"),
        @ApiResponse(responseCode = "409", description = "Conflicting rule after update")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "New extra fee rule state",
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            examples =
                                    @ExampleObject(
                                            name = "Phenomenon (rain) for cars",
                                            value =
                                                    """
                                                    {"conditionType":"PHENOMENON","vehicleType":"CAR","phenomenonCategory":"RAIN","fee":0.50,"validFrom":"2026-01-01T00:00:00Z"}
                                                    """)))
    @PutMapping("/{id}")
    public ExtraFeeRuleResponse update(
            @PathVariable Long id, @Valid @RequestBody ExtraFeeRuleRequest request) {
        return feeRuleService.updateExtraFee(id, request);
    }

    /**
     * @param id rule id
     */
    @Operation(summary = "Delete an extra fee rule")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Rule deleted"),
        @ApiResponse(responseCode = "404", description = "Unknown rule id")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        feeRuleService.deleteExtraFee(id);
    }
    // endregion
}
