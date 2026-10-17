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

    /**
     * The condition payload must match the condition type: numeric bounds for temperature/wind, a
     * phenomenon category for phenomenon (and never both).
     */
    @JsonIgnore
    @AssertTrue(
            message =
                    "condition payload must match conditionType: numeric bounds for"
                            + " TEMPERATURE/WIND, phenomenonCategory for PHENOMENON")
    public boolean isConditionPayloadValid() {
        if (conditionType == null) {
            return true; // @NotNull reports the actual problem
        }
        if (conditionType == ConditionType.PHENOMENON) {
            return phenomenonCategory != null && minValue == null && maxValue == null;
        }
        return phenomenonCategory == null && (minValue != null || maxValue != null);
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
        return validFrom == null || validTo == null || validTo.isAfter(validFrom);
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

    /** {@code minValue} must not exceed {@code maxValue}. */
    @JsonIgnore
    @AssertTrue(message = "minValue must not exceed maxValue")
    public boolean isRangeValid() {
        if (minValue == null || maxValue == null) {
            return true;
        }
        return minValue.compareTo(maxValue) <= 0;
    }
}
