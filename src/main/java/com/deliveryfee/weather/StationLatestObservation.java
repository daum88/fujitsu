package com.deliveryfee.weather;

import java.time.Instant;

/** Type-safe projection of the newest observation time per station. */
public interface StationLatestObservation {

    /**
     * @return the weather station name
     */
    String getStationName();

    /**
     * @return the newest observation instant for that station
     */
    Instant getObservedAt();
}
