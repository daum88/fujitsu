package com.deliveryfee.weather;

import static com.deliveryfee.common.City.TARTU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.deliveryfee.TestFixtures;
import com.deliveryfee.common.NoWeatherDataException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherObservationServiceTest {

    private static final Instant AT = Instant.parse("2026-10-14T12:00:00Z");

    @Mock private WeatherObservationRepository repository;

    private WeatherObservationService service;

    @BeforeEach
    void setUp() {
        service = new WeatherObservationService(repository);
    }

    @Test
    void latestFor_returnsObservation() {
        WeatherObservation observation =
                TestFixtures.observation(
                        "Tartu-Tõravere", new BigDecimal("-5.0"), null, null, AT.minusSeconds(60));
        when(repository.findTop1ByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                        anyString(), any()))
                .thenReturn(Optional.of(observation));

        assertThat(service.latestFor(TARTU, AT)).isSameAs(observation);
    }

    @Test
    void latestFor_missingObservation_throwsNoWeatherData() {
        when(repository.findTop1ByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(
                        anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.latestFor(TARTU, AT))
                .isInstanceOf(NoWeatherDataException.class);
    }
}
