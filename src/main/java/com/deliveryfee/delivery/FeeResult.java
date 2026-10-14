package com.deliveryfee.delivery;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The result of evaluating one fee component: either a monetary {@link Fee} or a {@link Forbidden}
 * outcome that short-circuits the whole calculation.
 */
public sealed interface FeeResult permits FeeResult.Fee, FeeResult.Forbidden {

    /**
     * A monetary fee for one fee source.
     *
     * @param amount the fee amount (never null)
     * @param type which fee source produced it
     */
    record Fee(BigDecimal amount, FeeType type) implements FeeResult {
        public Fee {
            Objects.requireNonNull(amount, "amount must not be null");
            Objects.requireNonNull(type, "type must not be null");
        }
    }

    /**
     * A rule forbidding the vehicle.
     *
     * @param type which fee source produced the forbidden outcome
     * @param message the verbatim, user-facing message
     */
    record Forbidden(FeeType type, String message) implements FeeResult {
        public Forbidden {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(message, "message must not be null");
        }
    }
}
