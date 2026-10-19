package com.deliveryfee.weather;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * In-memory record of the weather import's health: when it last succeeded and the last failure (if
 * any). Exposed through {@link WeatherImportHealthIndicator} and the import logs.
 */
@Component
public class ImportStatus {

    private volatile Instant lastSuccessAt;
    private volatile Instant lastAttemptAt;
    private volatile String lastError;

    /** Records a successful import run. */
    public synchronized void recordSuccess() {
        var now = Instant.now();
        this.lastSuccessAt = now;
        this.lastAttemptAt = now;
        this.lastError = null;
    }

    /**
     * Records a failed import run.
     *
     * @param message a short description of the failure
     */
    public synchronized void recordFailure(String message) {
        this.lastAttemptAt = Instant.now();
        this.lastError = message;
    }

    /**
     * @return the time of the last successful import, if any
     */
    public Optional<Instant> lastSuccessAt() {
        return Optional.ofNullable(lastSuccessAt);
    }

    /**
     * @return the time of the last attempt (successful or not), if any
     */
    public Optional<Instant> lastAttemptAt() {
        return Optional.ofNullable(lastAttemptAt);
    }

    /**
     * @return the last failure message, if any
     */
    public Optional<String> lastError() {
        return Optional.ofNullable(lastError);
    }
}
