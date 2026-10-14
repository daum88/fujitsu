package com.deliveryfee.delivery.matcher;

import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.observation;
import static com.deliveryfee.TestFixtures.temperatureRule;
import static com.deliveryfee.TestFixtures.windRule;
import static com.deliveryfee.common.VehicleType.BIKE;
import static org.assertj.core.api.Assertions.assertThat;

import com.deliveryfee.common.City;
import com.deliveryfee.delivery.DeliveryContext;
import com.deliveryfee.rules.ExtraFeeRule;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NumericRangeMatcherTest {

    private static final Instant AT = Instant.parse("2026-10-14T12:00:00Z");

    private final NumericRangeMatcher matcher = new NumericRangeMatcher();

    @ParameterizedTest(name = "value {4} against [min={0} inc={1}, max={2} inc={3}] matches={5}")
    @MethodSource("numericBoundaryCases")
    void matches_numericBoundaries(
            String min,
            Boolean minInclusive,
            String max,
            Boolean maxInclusive,
            String value,
            boolean expected) {
        ExtraFeeRule rule =
                temperatureRule(BIKE, bd(min), minInclusive, bd(max), maxInclusive, "1.00");
        DeliveryContext ctx = ctxWithTemperature(value);

        assertThat(matcher.matches(rule, ctx)).isEqualTo(expected);
    }

    @Test
    void matches_windRuleReadsWindSpeedNotTemperature() {
        ExtraFeeRule rule = windRule(BIKE, bd("10"), true, bd("20"), true, "0.50", false);
        DeliveryContext ctx =
                new DeliveryContext(
                        City.TALLINN,
                        BIKE,
                        AT,
                        observation("Tallinn-Harku", bd("-50"), bd("15"), null, AT));

        assertThat(matcher.matches(rule, ctx)).isTrue();
    }

    @Test
    void matches_nullWindSpeedNeverMatches() {
        ExtraFeeRule rule = windRule(BIKE, bd("10"), true, bd("20"), true, "0.50", false);
        DeliveryContext ctx =
                new DeliveryContext(
                        City.TALLINN,
                        BIKE,
                        AT,
                        observation("Tallinn-Harku", bd("5"), null, null, AT));

        assertThat(matcher.matches(rule, ctx)).isFalse();
    }

    private static DeliveryContext ctxWithTemperature(String temperature) {
        return new DeliveryContext(
                City.TARTU,
                BIKE,
                AT,
                observation("Tartu-Tõravere", bd(temperature), null, null, AT));
    }

    private static Stream<Arguments> numericBoundaryCases() {
        return Stream.of(
                // Air temperature rule "t < -10" (max=-10, exclusive)
                Arguments.of(null, null, "-10", false, "-10.1", true),
                Arguments.of(null, null, "-10", false, "-10.0", false),
                // Air temperature rule "-10 <= t <= 0"
                Arguments.of("-10", true, "0", true, "-10.0", true),
                Arguments.of("-10", true, "0", true, "-9.9", true),
                Arguments.of("-10", true, "0", true, "0.0", true),
                Arguments.of("-10", true, "0", true, "0.1", false),
                // Wind rule "10 <= w <= 20"
                Arguments.of("10", true, "20", true, "9.9", false),
                Arguments.of("10", true, "20", true, "10.0", true),
                Arguments.of("10", true, "20", true, "20.0", true),
                Arguments.of("10", true, "20", true, "20.1", false),
                // Wind rule "w > 20" (min=20, exclusive)
                Arguments.of("20", false, null, null, "20.0", false),
                Arguments.of("20", false, null, null, "20.1", true),
                // Unbounded range matches anything
                Arguments.of(null, null, null, null, "5.0", true));
    }
}
