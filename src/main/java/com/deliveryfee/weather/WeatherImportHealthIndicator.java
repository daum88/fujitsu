package com.deliveryfee.weather;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports the freshness of the weather import through {@code /actuator/health}. The service is
 * considered healthy only if an import has succeeded within the staleness threshold — otherwise it
 * is {@code DOWN} with the last success/failure details, giving operators a clear signal that the
 * upstream feed may be down or its contract may have changed.
 */
@Component
@RequiredArgsConstructor
public class WeatherImportHealthIndicator implements HealthIndicator {

    private static final Duration STALENESS_THRESHOLD = Duration.ofHours(3);

    private final ImportStatus importStatus;

    @Override
    public Health health() {
        var now = Instant.now();
        var lastSuccess = importStatus.lastSuccessAt();
        var fresh =
                lastSuccess.isPresent()
                        && lastSuccess.get().isAfter(now.minus(STALENESS_THRESHOLD));

        var builder = fresh ? Health.up() : Health.down();
        if (!fresh) {
            builder.withDetail("reason", "no recent successful weather import");
        }
        lastSuccess.ifPresent(t -> builder.withDetail("lastSuccessAt", t));
        importStatus.lastError().ifPresent(e -> builder.withDetail("lastError", e));
        return builder.build();
    }
}
