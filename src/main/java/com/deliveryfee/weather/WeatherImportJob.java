package com.deliveryfee.weather;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduling entry point for the weather import.
 *
 * <p>Runs hourly on the configured cron and performs a best-effort import at startup so a fresh
 * demo has data immediately. Failures are logged and never block application boot — the service
 * keeps serving its last-known history.
 */
@Component
@ConditionalOnProperty(
        name = "app.weather-import.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class WeatherImportJob {

    // region Constants
    private static final String LOG_PREFIX = "weather-import";
    // endregion

    // region Dependencies
    private final WeatherImportService importService;
    private final ImportStatus importStatus;

    // endregion

    // region Public API
    /** Hourly import driven by the configured cron expression. */
    @Scheduled(cron = "${app.weather-import.cron}")
    public void scheduledImport() {
        runImport();
    }

    /** Best-effort initial import once the application context is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void importOnStartup() {
        runImport();
    }

    // endregion

    // region Private helpers
    private void runImport() {
        long start = System.nanoTime();
        try {
            importService.importLatest();
            importStatus.recordSuccess();
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            importStatus.recordFailure(e.getMessage());
            log.error(
                    "{}: import failed after {}ms; serving last-known data. Cause: {}",
                    LOG_PREFIX,
                    durationMs,
                    e.toString(),
                    e);
        }
    }
    // endregion
}
