package com.deliveryfee.weather;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An append-only weather observation for a single station at a single point in time.
 *
 * <p>Rows are never updated or deleted; the unique constraint on {@code (station_name,
 * observed_at)} guarantees idempotent imports and doubles as the lookup index.
 */
@Entity
@Table(name = "weather_observation")
@Getter
@Setter
@NoArgsConstructor
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;

    @Column(name = "wmo_code", length = 20)
    private String wmoCode;

    @Column(name = "air_temperature", nullable = false, precision = 4, scale = 1)
    private BigDecimal airTemperature;

    @Column(name = "wind_speed", precision = 4, scale = 1)
    private BigDecimal windSpeed;

    @Column(name = "phenomenon", length = 100)
    private String phenomenon;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    /**
     * Creates a new observation.
     *
     * @param stationName station name (never null)
     * @param wmoCode WMO code, may be {@code null}
     * @param airTemperature air temperature in °C (never null)
     * @param windSpeed wind speed in m/s, may be {@code null}
     * @param phenomenon raw phenomenon text, may be {@code null}
     * @param observedAt observation instant (never null)
     */
    public WeatherObservation(
            String stationName,
            String wmoCode,
            BigDecimal airTemperature,
            BigDecimal windSpeed,
            String phenomenon,
            Instant observedAt) {
        this.stationName = stationName;
        this.wmoCode = wmoCode;
        this.airTemperature = airTemperature;
        this.windSpeed = windSpeed;
        this.phenomenon = phenomenon;
        this.observedAt = observedAt;
    }
}
