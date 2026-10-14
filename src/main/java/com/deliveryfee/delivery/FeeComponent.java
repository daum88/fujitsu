package com.deliveryfee.delivery;

/** A single, independently testable source of a delivery fee. */
public interface FeeComponent {

    /**
     * Evaluates this fee source for the given context.
     *
     * @param ctx the delivery context (city, vehicle, time, observation)
     * @return a fee or a forbidden outcome, never {@code null}
     */
    FeeResult evaluate(DeliveryContext ctx);
}
