package com.deliveryfee.delivery.dto;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * The fee response returned by the delivery fee endpoint.
 *
 * @param city delivery city
 * @param vehicleType vehicle type
 * @param currency ISO currency code (always EUR)
 * @param totalFee total delivery fee
 * @param calculatedAt the instant the fee was calculated for
 * @param weatherObservedAt the instant the used weather observation was recorded
 * @param breakdown itemized fee components
 */
public record DeliveryFeeResponse(
        City city,
        VehicleType vehicleType,
        String currency,
        BigDecimal totalFee,
        Instant calculatedAt,
        Instant weatherObservedAt,
        FeeBreakdown breakdown) {

    /**
     * @param city delivery city
     * @param vehicleType vehicle type
     * @param breakdown the itemized fee
     * @param calculatedAt the instant the fee was calculated for
     * @param weatherObservedAt the instant the used observation was recorded
     * @return the assembled response
     */
    public static DeliveryFeeResponse of(
            City city,
            VehicleType vehicleType,
            FeeBreakdown breakdown,
            Instant calculatedAt,
            Instant weatherObservedAt) {
        return new DeliveryFeeResponse(
                city,
                vehicleType,
                "EUR",
                breakdown.total(),
                calculatedAt,
                weatherObservedAt,
                breakdown);
    }
}
