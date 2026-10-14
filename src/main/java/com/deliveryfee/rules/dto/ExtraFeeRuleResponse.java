package com.deliveryfee.rules.dto;

import com.deliveryfee.common.ConditionType;
import com.deliveryfee.common.PhenomenonCategory;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.rules.ExtraFeeRule;
import java.math.BigDecimal;
import java.time.Instant;

/** Read model for an extra fee rule. */
public record ExtraFeeRuleResponse(
        Long id,
        ConditionType conditionType,
        VehicleType vehicleType,
        BigDecimal minValue,
        Boolean minInclusive,
        BigDecimal maxValue,
        Boolean maxInclusive,
        PhenomenonCategory phenomenonCategory,
        BigDecimal fee,
        boolean forbidden,
        Instant validFrom,
        Instant validTo) {

    /**
     * @param rule the entity to project
     * @return the DTO
     */
    public static ExtraFeeRuleResponse from(ExtraFeeRule rule) {
        return new ExtraFeeRuleResponse(
                rule.getId(),
                rule.getConditionType(),
                rule.getVehicleType(),
                rule.getMinValue(),
                rule.getMinInclusive(),
                rule.getMaxValue(),
                rule.getMaxInclusive(),
                rule.getPhenomenonCategory(),
                rule.getFee(),
                rule.isForbidden(),
                rule.getValidFrom(),
                rule.getValidTo());
    }
}
