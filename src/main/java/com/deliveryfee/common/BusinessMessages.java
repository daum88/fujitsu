package com.deliveryfee.common;

/**
 * Verbatim, user-facing business messages shared by the fee engine, the global exception handler
 * and the tests so that every layer references the exact same string.
 */
public final class BusinessMessages {

    /** Detail message returned when an active extra fee rule forbids the requested vehicle. */
    public static final String FORBIDDEN_VEHICLE = "Usage of selected vehicle type is forbidden";

    private BusinessMessages() {}
}
