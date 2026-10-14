package com.deliveryfee;

import com.deliveryfee.common.City;
import com.deliveryfee.common.ConditionType;
import com.deliveryfee.common.PhenomenonCategory;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.rules.BaseFeeRule;
import com.deliveryfee.rules.ExtraFeeRule;
import com.deliveryfee.weather.WeatherObservation;
import java.math.BigDecimal;
import java.time.Instant;

/** Shared factory for building test entities and rules with a fixed, open-ended epoch. */
public final class TestFixtures {

    /** Fixed validity start used by all seeded-style fixtures. */
    public static final Instant EPOCH = Instant.parse("2000-01-01T00:00:00Z");

    private TestFixtures() {}

    /** Converts a decimal string to {@link BigDecimal}, {@code null} stays {@code null}. */
    public static BigDecimal bd(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    /** Creates an observation for a station. */
    public static WeatherObservation observation(
            String station,
            BigDecimal airTemperature,
            BigDecimal windSpeed,
            String phenomenon,
            Instant observedAt) {
        return new WeatherObservation(
                station, null, airTemperature, windSpeed, phenomenon, observedAt);
    }

    /** Creates a base fee rule. */
    public static BaseFeeRule baseRule(
            City city, VehicleType vehicle, String fee, Instant validFrom, Instant validTo) {
        return new BaseFeeRule(city, vehicle, bd(fee), validFrom, validTo);
    }

    /** Creates a temperature extra fee rule. */
    public static ExtraFeeRule temperatureRule(
            VehicleType vehicle,
            BigDecimal min,
            Boolean minInclusive,
            BigDecimal max,
            Boolean maxInclusive,
            String fee) {
        return new ExtraFeeRule(
                ConditionType.TEMPERATURE,
                vehicle,
                min,
                minInclusive,
                max,
                maxInclusive,
                null,
                bd(fee),
                false,
                EPOCH,
                null);
    }

    /** Creates a wind extra fee rule. */
    public static ExtraFeeRule windRule(
            VehicleType vehicle,
            BigDecimal min,
            Boolean minInclusive,
            BigDecimal max,
            Boolean maxInclusive,
            String fee,
            boolean forbidden) {
        return new ExtraFeeRule(
                ConditionType.WIND,
                vehicle,
                min,
                minInclusive,
                max,
                maxInclusive,
                null,
                bd(fee),
                forbidden,
                EPOCH,
                null);
    }

    /** Creates a phenomenon extra fee rule. */
    public static ExtraFeeRule phenomenonRule(
            VehicleType vehicle, PhenomenonCategory category, String fee, boolean forbidden) {
        return new ExtraFeeRule(
                ConditionType.PHENOMENON,
                vehicle,
                null,
                null,
                null,
                null,
                category,
                bd(fee),
                forbidden,
                EPOCH,
                null);
    }
}
