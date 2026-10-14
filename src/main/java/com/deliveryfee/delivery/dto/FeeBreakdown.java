package com.deliveryfee.delivery.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Itemized delivery fee. Extra fees are always non-null (zero when not applicable); the regional
 * base fee is mandatory.
 */
public record FeeBreakdown(
        BigDecimal regionalBaseFee,
        BigDecimal airTemperatureExtraFee,
        BigDecimal windSpeedExtraFee,
        BigDecimal weatherPhenomenonExtraFee) {

    /**
     * Creates a breakdown, normalizing null extra fees to zero.
     *
     * @param regionalBaseFee regional base fee (must not be null)
     * @param airTemperatureExtraFee air temperature extra fee, may be null
     * @param windSpeedExtraFee wind speed extra fee, may be null
     * @param weatherPhenomenonExtraFee phenomenon extra fee, may be null
     * @return the normalized breakdown
     */
    public static FeeBreakdown of(
            BigDecimal regionalBaseFee,
            BigDecimal airTemperatureExtraFee,
            BigDecimal windSpeedExtraFee,
            BigDecimal weatherPhenomenonExtraFee) {
        Objects.requireNonNull(regionalBaseFee, "regionalBaseFee must not be null");
        return new FeeBreakdown(
                regionalBaseFee,
                zeroIfNull(airTemperatureExtraFee),
                zeroIfNull(windSpeedExtraFee),
                zeroIfNull(weatherPhenomenonExtraFee));
    }

    /**
     * @return the total fee as the sum of all four components
     */
    public BigDecimal total() {
        return regionalBaseFee
                .add(airTemperatureExtraFee)
                .add(windSpeedExtraFee)
                .add(weatherPhenomenonExtraFee);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
