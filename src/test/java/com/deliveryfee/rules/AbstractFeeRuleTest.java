package com.deliveryfee.rules;

import static com.deliveryfee.TestFixtures.baseRule;
import static com.deliveryfee.common.City.TALLINN;
import static com.deliveryfee.common.VehicleType.CAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AbstractFeeRuleTest {

    private static final Instant FROM = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2020-06-01T00:00:00Z");

    @Test
    void isActiveAt_validFromIsInclusive() {
        BaseFeeRule rule = baseRule(TALLINN, CAR, "4.00", FROM, null);

        assertThat(rule.isActiveAt(FROM)).isTrue();
    }

    @Test
    void isActiveAt_validToIsExclusive() {
        BaseFeeRule rule = baseRule(TALLINN, CAR, "4.00", FROM, TO);

        assertThat(rule.isActiveAt(TO)).isFalse();
        assertThat(rule.isActiveAt(TO.minusSeconds(1))).isTrue();
    }

    @Test
    void isActiveAt_beforeWindowIsInactive() {
        BaseFeeRule rule = baseRule(TALLINN, CAR, "4.00", FROM, TO);

        assertThat(rule.isActiveAt(FROM.minusSeconds(1))).isFalse();
    }

    @Test
    void isActiveAt_nullValidToIsOpenEnded() {
        BaseFeeRule rule = baseRule(TALLINN, CAR, "4.00", FROM, null);

        assertThat(rule.isActiveAt(Instant.parse("2999-01-01T00:00:00Z"))).isTrue();
    }
}
