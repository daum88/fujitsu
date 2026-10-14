package com.deliveryfee.rules;

import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.phenomenonRule;
import static com.deliveryfee.TestFixtures.temperatureRule;
import static com.deliveryfee.common.PhenomenonCategory.RAIN;
import static com.deliveryfee.common.PhenomenonCategory.SNOW_SLEET;
import static com.deliveryfee.common.VehicleType.BIKE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExtraFeeRuleTest {

    @Test
    void overlapsCondition_disjointRanges_returnsFalse() {
        ExtraFeeRule a = temperatureRule(BIKE, bd("-20"), true, bd("-10"), true, "1.00");
        ExtraFeeRule b = temperatureRule(BIKE, bd("0"), true, bd("10"), true, "1.00");

        assertThat(a.overlapsCondition(b)).isFalse();
    }

    @Test
    void overlapsCondition_overlappingRanges_returnsTrue() {
        ExtraFeeRule a = temperatureRule(BIKE, bd("-10"), true, bd("10"), true, "1.00");
        ExtraFeeRule b = temperatureRule(BIKE, bd("0"), true, bd("20"), true, "1.00");

        assertThat(a.overlapsCondition(b)).isTrue();
    }

    @Test
    void overlapsCondition_touchingBoundaryBothInclusive_returnsTrue() {
        ExtraFeeRule a = temperatureRule(BIKE, bd("-10"), true, bd("0"), true, "1.00");
        ExtraFeeRule b = temperatureRule(BIKE, bd("0"), true, bd("10"), true, "1.00");

        assertThat(a.overlapsCondition(b)).isTrue();
    }

    @Test
    void overlapsCondition_touchingBoundaryOneExclusive_returnsFalse() {
        ExtraFeeRule a = temperatureRule(BIKE, bd("-10"), true, bd("0"), true, "1.00");
        ExtraFeeRule b = temperatureRule(BIKE, bd("0"), false, bd("10"), true, "1.00");

        assertThat(a.overlapsCondition(b)).isFalse();
    }

    @Test
    void overlapsCondition_unboundedRanges_returnsTrue() {
        ExtraFeeRule a = temperatureRule(BIKE, bd("-10"), true, null, null, "1.00");
        ExtraFeeRule b = temperatureRule(BIKE, null, null, bd("0"), true, "1.00");

        assertThat(a.overlapsCondition(b)).isTrue();
    }

    @Test
    void overlapsCondition_unboundedDisjoint_returnsFalse() {
        ExtraFeeRule a = temperatureRule(BIKE, null, null, bd("-10"), true, "1.00");
        ExtraFeeRule b = temperatureRule(BIKE, bd("0"), true, null, null, "1.00");

        assertThat(a.overlapsCondition(b)).isFalse();
    }

    @Test
    void overlapsCondition_samePhenomenonCategory_returnsTrue() {
        ExtraFeeRule a = phenomenonRule(BIKE, SNOW_SLEET, "1.00", false);
        ExtraFeeRule b = phenomenonRule(BIKE, SNOW_SLEET, "1.50", false);

        assertThat(a.overlapsCondition(b)).isTrue();
    }

    @Test
    void overlapsCondition_differentPhenomenonCategory_returnsFalse() {
        ExtraFeeRule a = phenomenonRule(BIKE, SNOW_SLEET, "1.00", false);
        ExtraFeeRule b = phenomenonRule(BIKE, RAIN, "1.00", false);

        assertThat(a.overlapsCondition(b)).isFalse();
    }

    @Test
    void overlapsCondition_numericVersusPhenomenon_returnsFalse() {
        ExtraFeeRule a = temperatureRule(BIKE, bd("-10"), true, bd("0"), true, "1.00");
        ExtraFeeRule b = phenomenonRule(BIKE, SNOW_SLEET, "1.00", false);

        assertThat(a.overlapsCondition(b)).isFalse();
    }
}
