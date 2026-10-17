package com.deliveryfee.weather;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Data access for {@link WeatherObservation}. Only derived queries and parameterized JPQL are used
 * — never string-concatenated SQL.
 */
public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {

    /**
     * Finds the newest observation for a station at or before the given instant.
     *
     * @param stationName station name
     * @param observedAt upper bound (inclusive)
     * @return the newest matching observation, if any
     */
    Optional<WeatherObservation>
            findTop1ByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                    String stationName, Instant observedAt);

    /**
     * Returns the newest {@code observedAt} per station in a single grouped query.
     *
     * <p>Each element is {@code [stationName, max(observedAt)]}. Used to skip already-imported feed
     * entries without an N+1 lookup.
     *
     * @return one row per station with its latest observation time
     */
    @Query(
            "select w.stationName, max(w.observedAt) from WeatherObservation w group by w.stationName")
    List<Object[]> findLatestObservedAtPerStation();

    /**
     * Checks whether an observation for a station and instant already exists (idempotent seeding).
     *
     * @param stationName station name
     * @param observedAt observation instant
     * @return {@code true} when the observation is already stored
     */
    boolean existsByStationNameAndObservedAt(String stationName, Instant observedAt);
}
