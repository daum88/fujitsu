package com.deliveryfee.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The three delivery cities. Each city maps to its Estonian weather station name, which is the
 * single source of truth used by both the import filter and the fee lookup.
 */
public enum City {
    TALLINN("Tallinn-Harku"),
    TARTU("Tartu-Tõravere"),
    PARNU("Pärnu");

    private final String stationName;

    City(String stationName) {
        this.stationName = stationName;
    }

    /**
     * @return the weather station name observed for this city
     */
    public String getStationName() {
        return stationName;
    }

    private static final Set<String> STATION_NAMES =
            Arrays.stream(values())
                    .map(City::getStationName)
                    .collect(Collectors.toUnmodifiableSet());

    /**
     * Checks whether a station name belongs to one of the configured cities.
     *
     * @param name the station name from the weather feed
     * @return {@code true} when the station is relevant to fee calculation
     */
    public static boolean isKnownStation(String name) {
        return name != null && STATION_NAMES.contains(name);
    }

    /**
     * @return the immutable set of relevant station names
     */
    public static Set<String> stationNames() {
        return STATION_NAMES;
    }
}
