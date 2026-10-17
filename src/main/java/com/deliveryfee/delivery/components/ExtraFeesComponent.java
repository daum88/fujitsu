package com.deliveryfee.delivery.components;

import com.deliveryfee.common.BusinessMessages;
import com.deliveryfee.common.ConditionType;
import com.deliveryfee.delivery.DeliveryContext;
import com.deliveryfee.delivery.FeeComponent;
import com.deliveryfee.delivery.FeeResult;
import com.deliveryfee.delivery.FeeType;
import com.deliveryfee.delivery.matcher.ConditionMatcher;
import com.deliveryfee.rules.ExtraFeeRule;
import com.deliveryfee.rules.ExtraFeeRuleRepository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Evaluates one extra fee condition (temperature, wind or phenomenon) against the active extra fee
 * rules for the vehicle. One class serves all three conditions; a fourth condition would need new
 * rows plus a matcher only if its matching logic differs.
 */
public class ExtraFeesComponent implements FeeComponent {

    // region Dependencies
    private final ConditionType conditionType;
    private final ConditionMatcher matcher;
    private final ExtraFeeRuleRepository extraFeeRuleRepository;

    // endregion

    // region Public API
    /**
     * @param conditionType which weather condition this component evaluates
     * @param matcher strategy deciding whether a rule's condition matches the observation
     * @param extraFeeRuleRepository source of extra fee rules
     */
    public ExtraFeesComponent(
            ConditionType conditionType,
            ConditionMatcher matcher,
            ExtraFeeRuleRepository extraFeeRuleRepository) {
        this.conditionType = conditionType;
        this.matcher = matcher;
        this.extraFeeRuleRepository = extraFeeRuleRepository;
    }

    @Override
    public FeeResult evaluate(DeliveryContext ctx) {
        List<ExtraFeeRule> rules =
                extraFeeRuleRepository.findActiveRules(
                        ctx.vehicle(), conditionType, ctx.requestedAt());
        for (ExtraFeeRule rule : rules) {
            if (!matcher.matches(rule, ctx)) {
                continue;
            }
            FeeType type = feeTypeFor(conditionType);
            if (rule.isForbidden()) {
                return new FeeResult.Forbidden(type, BusinessMessages.FORBIDDEN_VEHICLE);
            }
            return new FeeResult.Fee(rule.getFee(), type);
        }
        return new FeeResult.Fee(BigDecimal.ZERO, feeTypeFor(conditionType));
    }

    // endregion

    // region Private helpers
    private static FeeType feeTypeFor(ConditionType conditionType) {
        return switch (conditionType) {
            case TEMPERATURE -> FeeType.AIR_TEMPERATURE;
            case WIND -> FeeType.WIND_SPEED;
            case PHENOMENON -> FeeType.WEATHER_PHENOMENON;
        };
    }
    // endregion
}
