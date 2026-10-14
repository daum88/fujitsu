package com.deliveryfee.delivery.matcher;

import com.deliveryfee.common.PhenomenonCategory;
import com.deliveryfee.delivery.DeliveryContext;
import com.deliveryfee.rules.ExtraFeeRule;

/**
 * Matches phenomenon extra fee rules by classifying the observation's phenomenon text (with
 * declaration-order precedence) and comparing it to the rule's category.
 */
public class PhenomenonMatcher implements ConditionMatcher {

    @Override
    public boolean matches(ExtraFeeRule rule, DeliveryContext ctx) {
        PhenomenonCategory category =
                PhenomenonCategory.classify(ctx.observation().getPhenomenon());
        return category != null && category == rule.getPhenomenonCategory();
    }
}
