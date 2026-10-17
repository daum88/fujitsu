package com.deliveryfee.weather;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dev-only seeder that backfills a few historical weather observations so the historical {@code
 * datetime} feature can be exercised immediately without waiting for live imports to accumulate.
 * Gated behind {@code app.demo-data.enabled} (on in the {@code dev} profile).
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private static final Instant WINTER = Instant.parse("2024-01-15T10:00:00Z");
    private static final Instant STORM = Instant.parse("2024-07-15T10:00:00Z");

    private final WeatherObservationRepository weatherRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedWinter();
        seedStorm();
        log.info("demo-data: seeded historical observations for scenario testing");
    }

    private void seedWinter() {
        insert("Tallinn-Harku", "-5.0", "5.0", "Light snow shower", WINTER);
        insert("Tartu-Tõravere", "-12.0", "4.0", "Snow", WINTER);
        insert("Pärnu", "1.0", "15.0", "Moderate rain", WINTER);
    }

    private void seedStorm() {
        insert("Tallinn-Harku", "20.0", "22.0", "Thunderstorm", STORM);
        insert("Pärnu", "25.0", "8.0", "Cloudy with clear spells", STORM);
    }

    private void insert(
            String station, String temperature, String wind, String phenomenon, Instant at) {
        if (weatherRepository.existsByStationNameAndObservedAt(station, at)) {
            return;
        }
        weatherRepository.save(
                new WeatherObservation(
                        station,
                        null,
                        new BigDecimal(temperature),
                        new BigDecimal(wind),
                        phenomenon,
                        at));
    }
}
