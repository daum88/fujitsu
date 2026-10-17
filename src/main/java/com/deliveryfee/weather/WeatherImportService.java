package com.deliveryfee.weather;

import com.deliveryfee.common.City;
import com.deliveryfee.weather.xml.ObservationsXml;
import com.deliveryfee.weather.xml.StationXml;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Orchestrates the hourly weather import: fetch the feed, parse it with the hardened XML mapper,
 * filter to the three relevant stations, and append only the observations that are newer than what
 * is already stored.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherImportService {

    private static final String LOG_PREFIX = "weather-import";

    private final RestClient weatherRestClient;
    private final XmlMapper xmlMapper;
    private final WeatherObservationRepository weatherRepository;

    /**
     * Imports the latest observations from the configured weather feed. The operation is
     * append-only and idempotent: entries already stored (or missing required data) are skipped.
     *
     * @return a summary of imported, skipped and missing stations
     * @throws IllegalStateException when the feed cannot be fetched or parsed
     */
    @Transactional
    public ImportResult importLatest() {
        ObservationsXml observations = parse(fetchFeed());
        if (observations.timestamp() <= 0) {
            throw new IllegalStateException("Weather feed timestamp is missing or invalid");
        }
        Instant observedAt = Instant.ofEpochSecond(observations.timestamp());

        List<StationXml> stations =
                observations.stations() == null ? List.of() : observations.stations();
        List<StationXml> relevantStations =
                stations.stream().filter(station -> City.isKnownStation(station.name())).toList();

        Map<String, Instant> latestPerStation = latestObservedAtPerStation();
        Set<String> missingStations = new HashSet<>(City.stationNames());

        List<WeatherObservation> newObservations = new ArrayList<>();
        Set<String> alreadySeen = new HashSet<>();
        int skipped = 0;
        for (StationXml station : relevantStations) {
            missingStations.remove(station.name());

            // Defensive: a feed may list the same station twice; only the first occurrence is kept
            // so the unique (station, observed_at) constraint is never violated by one batch.
            if (!alreadySeen.add(station.name())) {
                skipped++;
                continue;
            }

            Instant latest = latestPerStation.get(station.name());
            if (latest != null && !observedAt.isAfter(latest)) {
                skipped++;
                continue;
            }

            BigDecimal temperature = parseBigDecimal(station.airTemperature());
            if (temperature == null) {
                log.warn("{}: {} missing air temperature, skipped", LOG_PREFIX, station.name());
                skipped++;
                continue;
            }

            newObservations.add(toObservation(station, temperature, observedAt));
        }

        weatherRepository.saveAll(newObservations);
        log.info(
                "{}: imported={} skipped={} missingStations={}",
                LOG_PREFIX,
                newObservations.size(),
                skipped,
                missingStations);
        return new ImportResult(newObservations.size(), skipped, List.copyOf(missingStations));
    }

    private String fetchFeed() {
        byte[] body = weatherRestClient.get().retrieve().body(byte[].class);
        if (body == null || body.length == 0) {
            throw new IllegalStateException("Weather feed returned an empty response");
        }
        // The feed is UTF-8 (and declares so in its XML prolog). Decoding the raw bytes
        // explicitly keeps non-ASCII station names (Tõravere, Pärnu) intact regardless of
        // any missing/incorrect charset header.
        return new String(body, StandardCharsets.UTF_8);
    }

    private ObservationsXml parse(String xml) {
        try {
            return xmlMapper.readValue(xml, ObservationsXml.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse weather feed", e);
        }
    }

    private Map<String, Instant> latestObservedAtPerStation() {
        return weatherRepository.findLatestObservedAtPerStation().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Instant) row[1]));
    }

    private WeatherObservation toObservation(
            StationXml station, BigDecimal temperature, Instant observedAt) {
        return new WeatherObservation(
                station.name(),
                station.wmoCode(),
                temperature,
                parseBigDecimal(station.windSpeed()),
                station.phenomenon(),
                observedAt);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
