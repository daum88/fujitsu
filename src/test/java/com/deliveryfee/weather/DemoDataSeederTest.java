package com.deliveryfee.weather;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock private WeatherObservationRepository repository;

    private DemoDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DemoDataSeeder(repository);
    }

    @Test
    void run_seedsWinterAndStorm() {
        when(repository.existsByStationNameAndObservedAt(anyString(), any())).thenReturn(false);

        seeder.run(null);

        verify(repository, times(5)).save(any(WeatherObservation.class));
    }

    @Test
    void run_skipsAlreadyStoredObservations() {
        when(repository.existsByStationNameAndObservedAt(anyString(), any())).thenReturn(true);

        seeder.run(null);

        verify(repository, never()).save(any(WeatherObservation.class));
    }
}
