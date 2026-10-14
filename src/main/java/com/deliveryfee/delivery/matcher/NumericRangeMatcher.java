package com.deliveryfee.delivery.matcher;

import com.deliveryfee.common.ConditionType;
import com.deliveryfee.delivery.DeliveryContext;
import com.deliveryfee.rules.ExtraFeeRule;
import java.math.BigDecimal;

/**
 * Matches numeric extra fee rules (temperature and wind) against the observation value, honoring
 * the rule's inclusivity flags. A {@code null} observation value never matches.
 */
public class NumericRangeMatcher implements ConditionMatcher {

    @Override
    public boolean matches(ExtraFeeRule rule, DeliveryContext ctx) {
        BigDecimal value =
                rule.getConditionType() == ConditionType.TEMPERATURE
                        ? ctx.observation().getAirTemperature()
                        : ctx.observation().getWindSpeed();
        if (value == null) {
            return false;
        }
        return matchesMin(rule, value) && matchesMax(rule, value);
    }

    private boolean matchesMin(ExtraFeeRule rule, BigDecimal value) {
        if (rule.getMinValue() == null) {
            return true;
        }
        int comparison = value.compareTo(rule.getMinValue());
        return comparison > 0 || (comparison == 0 && Boolean.TRUE.equals(rule.getMinInclusive()));
    }

    private boolean matchesMax(ExtraFeeRule rule, BigDecimal value) {
        if (rule.getMaxValue() == null) {
            return true;
        }
        int comparison = value.compareTo(rule.getMaxValue());
        return comparison < 0 || (comparison == 0 && Boolean.TRUE.equals(rule.getMaxInclusive()));
    }
}
