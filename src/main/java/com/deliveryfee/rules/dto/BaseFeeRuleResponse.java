package com.deliveryfee.rules.dto;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.rules.BaseFeeRule;
import java.math.BigDecimal;
import java.time.Instant;

/** Read model for a base fee rule. */
public record BaseFeeRuleResponse(
        Long id,
        City city,
        VehicleType vehicleType,
        BigDecimal fee,
        Instant validFrom,
        Instant validTo) {

    /**
     * @param rule the entity to project
     * @return the DTO
     */
    public static BaseFeeRuleResponse from(BaseFeeRule rule) {
        return new BaseFeeRuleResponse(
                rule.getId(),
                rule.getCity(),
                rule.getVehicleType(),
                rule.getFee(),
                rule.getValidFrom(),
                rule.getValidTo());
    }
}
