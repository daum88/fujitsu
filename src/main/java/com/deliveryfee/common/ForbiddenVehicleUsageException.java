package com.deliveryfee.common;

/** Thrown when an active extra fee rule forbids the requested vehicle. Maps to HTTP 400. */
public class ForbiddenVehicleUsageException extends RuntimeException {

    /**
     * @param message the verbatim, user-facing reason (see {@link BusinessMessages})
     */
    public ForbiddenVehicleUsageException(String message) {
        super(message);
    }
}
