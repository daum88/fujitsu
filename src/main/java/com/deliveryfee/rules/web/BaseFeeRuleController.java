package com.deliveryfee.rules.web;

import com.deliveryfee.rules.FeeRuleService;
import com.deliveryfee.rules.dto.BaseFeeRuleRequest;
import com.deliveryfee.rules.dto.BaseFeeRuleResponse;
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

/** HTTP-only controller for base fee rule CRUD. */
@Tag(name = "Rules — Base Fees", description = "CRUD for regional base fee rules")
@RestController
@RequestMapping("/api/v1/rules/base-fees")
@RequiredArgsConstructor
public class BaseFeeRuleController {

    private final FeeRuleService feeRuleService;

    /**
     * @return all base fee rules
     */
    @Operation(summary = "List all base fee rules")
    @GetMapping
    public List<BaseFeeRuleResponse> list() {
        return feeRuleService.listBaseFees();
    }

    /**
     * @param request the rule to create
     * @return the created rule
     */
    @Operation(
            summary = "Create a base fee rule",
            description =
                    """
                    Adds a regional base fee. Validity windows are half-open [validFrom, validTo).
                    The window must not overlap an existing rule for the same city and vehicle —
                    overlapping windows are rejected with 409. Seeded rules are open-ended from
                    2000-01-01, so use a historical window to test a successful create.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rule created"),
        @ApiResponse(responseCode = "400", description = "Validation error (e.g. negative fee)"),
        @ApiResponse(
                responseCode = "409",
                description = "Overlapping rule for the same city and vehicle")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Base fee rule to create",
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            examples =
                                    @ExampleObject(
                                            name = "Historical rule (non-overlapping window)",
                                            value =
                                                    """
                                                    {"city":"TALLINN","vehicleType":"CAR","fee":5.00,"validFrom":"1990-01-01T00:00:00Z","validTo":"1995-01-01T00:00:00Z"}
                                                    """)))
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
    @Operation(summary = "Update a base fee rule", description = "Replaces the rule's fields.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule updated"),
        @ApiResponse(responseCode = "404", description = "Unknown rule id"),
        @ApiResponse(responseCode = "409", description = "Overlapping rule after update")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "New base fee rule state",
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            examples =
                                    @ExampleObject(
                                            name = "Update Tallinn/Car to 4.50",
                                            value =
                                                    """
                                                    {"city":"TALLINN","vehicleType":"CAR","fee":4.50,"validFrom":"2000-01-01T00:00:00Z"}
                                                    """)))
    @PutMapping("/{id}")
    public BaseFeeRuleResponse update(
            @PathVariable Long id, @Valid @RequestBody BaseFeeRuleRequest request) {
        return feeRuleService.updateBaseFee(id, request);
    }

    /**
     * @param id rule id
     */
    @Operation(summary = "Delete a base fee rule")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Rule deleted"),
        @ApiResponse(responseCode = "404", description = "Unknown rule id")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        feeRuleService.deleteBaseFee(id);
    }
}
