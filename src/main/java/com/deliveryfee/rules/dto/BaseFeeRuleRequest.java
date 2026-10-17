package com.deliveryfee.rules.dto;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** Create/update request for a base fee rule. */
public record BaseFeeRuleRequest(
        @NotNull City city,
        @NotNull VehicleType vehicleType,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal fee,
        @NotNull Instant validFrom,
        Instant validTo) {

    /** {@code validTo} must be after {@code validFrom} (half-open interval). */
    @JsonIgnore
    @AssertTrue(message = "validTo must be after validFrom")
    public boolean isValidityValid() {
        return validFrom == null || validTo == null || validTo.isAfter(validFrom);
    }
}
