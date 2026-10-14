package com.deliveryfee.common;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {

    /**
     * @param entity a human-readable entity type
     * @param id the identifier that was not found
     */
    public NotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}
