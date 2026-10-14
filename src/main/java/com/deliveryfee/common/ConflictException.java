package com.deliveryfee.common;

/** Thrown when a create/update would produce conflicting (overlapping) rules. Maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    /**
     * @param message description of the conflict
     */
    public ConflictException(String message) {
        super(message);
    }
}
