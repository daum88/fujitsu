package com.deliveryfee.delivery.components;

import com.deliveryfee.delivery.DeliveryContext;
import com.deliveryfee.delivery.FeeComponent;
import com.deliveryfee.delivery.FeeResult;
import com.deliveryfee.delivery.FeeType;
import com.deliveryfee.rules.BaseFeeRuleRepository;

/** Resolves the regional base fee (RBF) from the active base fee rule for the city and vehicle. */
public class RegionalBaseFeeComponent implements FeeComponent {

    private final BaseFeeRuleRepository baseFeeRuleRepository;

    public RegionalBaseFeeComponent(BaseFeeRuleRepository baseFeeRuleRepository) {
        this.baseFeeRuleRepository = baseFeeRuleRepository;
    }

    @Override
    public FeeResult evaluate(DeliveryContext ctx) {
        return baseFeeRuleRepository
                .findActiveRules(ctx.city(), ctx.vehicle(), ctx.requestedAt())
                .stream()
                .findFirst()
                .map(rule -> new FeeResult.Fee(rule.getFee(), FeeType.REGIONAL_BASE))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No active base fee rule for "
                                                + ctx.city()
                                                + "/"
                                                + ctx.vehicle()));
    }
}
