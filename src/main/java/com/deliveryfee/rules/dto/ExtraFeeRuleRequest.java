package com.deliveryfee.rules.dto;

import com.deliveryfee.common.ConditionType;
import com.deliveryfee.common.PhenomenonCategory;
import com.deliveryfee.common.VehicleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** Create/update request for an extra fee rule (temperature, wind or phenomenon). */
public record ExtraFeeRuleRequest(
        @NotNull ConditionType conditionType,
        @NotNull VehicleType vehicleType,
        BigDecimal minValue,
        Boolean minInclusive,
        BigDecimal maxValue,
        Boolean maxInclusive,
        PhenomenonCategory phenomenonCategory,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal fee,
        boolean forbidden,
        @NotNull Instant validFrom,
        Instant validTo) {

    /** Exactly one of numeric bounds or phenomenon category must be present. */
    @JsonIgnore
    @AssertTrue(message = "Exactly one of numeric bounds or phenomenon category must be present")
    public boolean isConditionSpecified() {
        boolean numeric = minValue != null || maxValue != null;
        boolean phenomenon = phenomenonCategory != null;
        return numeric ^ phenomenon;
    }

    /** A fee is required unless the rule forbids the vehicle. */
    @JsonIgnore
    @AssertTrue(message = "Fee is required unless the rule is forbidden")
    public boolean isFeeValid() {
        return forbidden || fee != null;
    }

    /** {@code validTo} must be after {@code validFrom}. */
    @JsonIgnore
    @AssertTrue(message = "validTo must be after validFrom")
    public boolean isValidityValid() {
        return validTo == null || validTo.isAfter(validFrom);
    }

    /** {@code minInclusive} is required when {@code minValue} is present. */
    @JsonIgnore
    @AssertTrue(message = "minInclusive is required when minValue is set")
    public boolean isMinInclusiveValid() {
        return minValue == null || minInclusive != null;
    }

    /** {@code maxInclusive} is required when {@code maxValue} is present. */
    @JsonIgnore
    @AssertTrue(message = "maxInclusive is required when maxValue is set")
    public boolean isMaxInclusiveValid() {
        return maxValue == null || maxInclusive != null;
    }
}
