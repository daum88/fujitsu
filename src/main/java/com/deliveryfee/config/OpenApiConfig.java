package com.deliveryfee.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI documentation configuration consumed by springdoc to render the Swagger UI. */
@Configuration
public class OpenApiConfig {

    /**
     * Defines the top-level OpenAPI document metadata.
     *
     * @return the configured {@link OpenAPI} bean
     */
    @Bean
    public OpenAPI deliveryFeeOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Delivery Fee Service API")
                                .version("1.0.0")
                                .description(
                                        "Calculates delivery fees from data-driven business rules"
                                                + " and Estonian weather observations."));
    }
}
