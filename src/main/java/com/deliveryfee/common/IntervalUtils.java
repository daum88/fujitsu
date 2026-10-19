package com.deliveryfee.common;

import java.time.Instant;
import java.util.Objects;

/**
 * Utilities for half-open validity intervals {@code [from, to)}. A {@code null} upper bound means
 * the interval is open-ended (no expiry).
 */
public final class IntervalUtils {

    private IntervalUtils() {}

    /**
     * Determines whether two half-open intervals {@code [from, to)} overlap.
     *
     * @param fromA start of interval A (inclusive, never null)
     * @param toA end of interval A (exclusive, null = open-ended)
     * @param fromB start of interval B (inclusive, never null)
     * @param toB end of interval B (exclusive, null = open-ended)
     * @return {@code true} when the intervals share at least one instant
     * @throws NullPointerException if either start bound is {@code null}
     */
    public static boolean overlaps(Instant fromA, Instant toA, Instant fromB, Instant toB) {
        Objects.requireNonNull(fromA, "fromA must not be null");
        Objects.requireNonNull(fromB, "fromB must not be null");
        var endA = toA == null ? Instant.MAX : toA;
        var endB = toB == null ? Instant.MAX : toB;
        return fromA.isBefore(endB) && fromB.isBefore(endA);
    }
}
