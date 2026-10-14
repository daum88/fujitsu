package com.deliveryfee.common;

import java.time.Instant;

/**
 * Thrown when no weather observation exists for a city at or before the requested time. Maps to
 * HTTP 422.
 */
public class NoWeatherDataException extends RuntimeException {

    /**
     * @param city the city for which no data is available
     * @param at the requested calculation instant
     */
    public NoWeatherDataException(City city, Instant at) {
        super("No weather data available for " + city + " at or before " + at);
    }
}
