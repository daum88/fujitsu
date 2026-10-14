package com.deliveryfee.delivery.matcher;

import com.deliveryfee.delivery.DeliveryContext;
import com.deliveryfee.rules.ExtraFeeRule;

/** Strategy that decides whether an extra fee rule's condition matches the current observation. */
public interface ConditionMatcher {

    /**
     * @param rule the extra fee rule under consideration
     * @param ctx the delivery context with the resolved observation
     * @return {@code true} when the rule's condition applies to the observation
     */
    boolean matches(ExtraFeeRule rule, DeliveryContext ctx);
}
