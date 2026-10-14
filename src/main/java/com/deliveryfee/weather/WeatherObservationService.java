package com.deliveryfee.weather;

import com.deliveryfee.common.City;
import com.deliveryfee.common.NoWeatherDataException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side service for weather observations: resolves the observation for a city at a time. */
@Service
@RequiredArgsConstructor
public class WeatherObservationService {

    private final WeatherObservationRepository weatherRepository;

    /**
     * Finds the newest observation for the city at or before the requested instant.
     *
     * @param city delivery city (determines the weather station)
     * @param at upper bound (inclusive) on the observation time
     * @return the newest matching observation, never {@code null}
     * @throws NoWeatherDataException when no observation exists at or before {@code at}
     */
    @Transactional(readOnly = true)
    public WeatherObservation latestFor(City city, Instant at) {
        return weatherRepository
                .findTop1ByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                        city.getStationName(), at)
                .orElseThrow(() -> new NoWeatherDataException(city, at));
    }
}
