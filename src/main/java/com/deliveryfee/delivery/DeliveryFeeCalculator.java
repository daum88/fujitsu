package com.deliveryfee.delivery;

import com.deliveryfee.common.ConditionType;
import com.deliveryfee.common.ForbiddenVehicleUsageException;
import com.deliveryfee.delivery.components.ExtraFeesComponent;
import com.deliveryfee.delivery.components.RegionalBaseFeeComponent;
import com.deliveryfee.delivery.dto.FeeBreakdown;
import com.deliveryfee.delivery.matcher.NumericRangeMatcher;
import com.deliveryfee.delivery.matcher.PhenomenonMatcher;
import com.deliveryfee.rules.BaseFeeRuleRepository;
import com.deliveryfee.rules.ExtraFeeRuleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Composite fee engine that folds the results of its fee components in order. A forbidden outcome
 * short-circuits the calculation with a {@link ForbiddenVehicleUsageException}.
 */
@Component
public class DeliveryFeeCalculator {

    // region Dependencies
    private final List<FeeComponent> components;

    // endregion

    // region Public API
    /**
     * @param baseFeeRuleRepository source of regional base fee rules
     * @param extraFeeRuleRepository source of extra fee rules
     */
    public DeliveryFeeCalculator(
            BaseFeeRuleRepository baseFeeRuleRepository,
            ExtraFeeRuleRepository extraFeeRuleRepository) {
        this.components =
                List.of(
                        new RegionalBaseFeeComponent(baseFeeRuleRepository),
                        new ExtraFeesComponent(
                                ConditionType.TEMPERATURE,
                                new NumericRangeMatcher(),
                                extraFeeRuleRepository),
                        new ExtraFeesComponent(
                                ConditionType.WIND,
                                new NumericRangeMatcher(),
                                extraFeeRuleRepository),
                        new ExtraFeesComponent(
                                ConditionType.PHENOMENON,
                                new PhenomenonMatcher(),
                                extraFeeRuleRepository));
    }

    /**
     * Calculates the full fee breakdown for the given context.
     *
     * @param ctx the delivery context
     * @return the fee breakdown with a non-null regional base fee and zero-based extra fees
     * @throws ForbiddenVehicleUsageException when any active rule forbids the vehicle
     */
    public FeeBreakdown calculate(DeliveryContext ctx) {
        BigDecimal regionalBase = null;
        var temperature = BigDecimal.ZERO;
        var wind = BigDecimal.ZERO;
        var phenomenon = BigDecimal.ZERO;

        for (var component : components) {
            var result = component.evaluate(ctx);
            if (result instanceof FeeResult.Forbidden forbidden) {
                throw new ForbiddenVehicleUsageException(forbidden.message());
            }
            var fee = (FeeResult.Fee) result;
            switch (fee.type()) {
                case REGIONAL_BASE -> regionalBase = fee.amount();
                case AIR_TEMPERATURE -> temperature = fee.amount();
                case WIND_SPEED -> wind = fee.amount();
                case WEATHER_PHENOMENON -> phenomenon = fee.amount();
                default -> throw new IllegalStateException("Unhandled fee type: " + fee.type());
            }
        }

        return FeeBreakdown.of(regionalBase, temperature, wind, phenomenon);
    }
    // endregion
}
