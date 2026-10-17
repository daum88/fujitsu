package com.deliveryfee.weather;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class WeatherImportHealthIndicatorTest {

    private final ImportStatus importStatus = new ImportStatus();
    private final WeatherImportHealthIndicator indicator =
            new WeatherImportHealthIndicator(importStatus);

    @Test
    void neverImported_isDown() {
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void recentSuccess_isUp() {
        importStatus.recordSuccess();

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void failureAfterSuccess_keepsLastSuccessAndReportsUp() {
        importStatus.recordSuccess();
        importStatus.recordFailure("feed unavailable");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(importStatus.lastError()).contains("feed unavailable");
    }
}
