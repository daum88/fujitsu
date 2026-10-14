package com.deliveryfee.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed, validated application configuration bound from {@code application.yml}.
 *
 * <p>The weather import URL is configuration-only (never request input), which together with
 * the HTTPS-only constraint mitigates SSRF.</p>
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid WeatherImport weatherImport) {

    /** Configuration for the hourly weather import. */
    public record WeatherImport(
            boolean enabled,
            @NotBlank String cron,
            @NotBlank String url,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout,
            @Min(1) int batchSize) {}
}
