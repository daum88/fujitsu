package com.deliveryfee;

import com.deliveryfee.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the delivery fee service.
 *
 * <p>Scheduling is enabled for the hourly weather import; the import itself is gated by
 * {@code app.weather-import.enabled} so it can be switched off in tests.</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class DeliveryFeeApplication {

    /**
     * Bootstraps the Spring application context.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DeliveryFeeApplication.class, args);
    }
}
