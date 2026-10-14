package com.deliveryfee.common;

/** Thrown for client input that fails business validation but is not covered by bean validation. */
public class BadRequestException extends RuntimeException {

    /**
     * @param message description of the invalid request
     */
    public BadRequestException(String message) {
        super(message);
    }
}
