package com.deliveryfee.delivery;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.delivery.dto.DeliveryFeeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP-only controller for the delivery fee endpoint. Delegates everything to the service. */
@Tag(name = "Delivery Fee", description = "Calculate delivery fees — current and historical")
@RestController
@RequestMapping("/api/v1/delivery-fee")
@RequiredArgsConstructor
@Validated
public class DeliveryFeeController {

    // region Dependencies
    private final DeliveryFeeService deliveryFeeService;

    // endregion

    // region Public API
    /**
     * Calculates the delivery fee.
     *
     * @param city delivery city (strict, case-sensitive enum value)
     * @param vehicleType vehicle type (strict, case-sensitive enum value)
     * @param datetime optional ISO-8601 instant; defaults to now, must not be in the future
     * @return the fee response
     */
    @Operation(
            summary = "Calculate a delivery fee",
            description =
                    """
                    Returns the total delivery fee and its itemized breakdown for a city and vehicle.
                    Omit `datetime` for the current fee, or pass an ISO-8601 instant (not in the
                    future) to price a historical delivery using the weather and rules that were
                    active at that time.

                    Historical weather is collected going forward from the first run. With the
                    `dev` profile, demo observations are seeded so you can also try the past:
                    - Current fee:   city=TARTU&vehicleType=BIKE
                    - Winter (snow): city=TARTU&vehicleType=BIKE&datetime=2024-01-15T12:00:00Z
                    - Summer storm:  city=TALLINN&vehicleType=BIKE&datetime=2024-07-15T12:00:00Z
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fee calculated"),
        @ApiResponse(
                responseCode = "400",
                description =
                        "Bad enum, missing parameter, malformed/future datetime, or forbidden vehicle"),
        @ApiResponse(
                responseCode = "422",
                description = "No weather data at or before the requested time")
    })
    @GetMapping
    public DeliveryFeeResponse calculate(
            @Parameter(description = "Delivery city", required = true, example = "TARTU")
                    @RequestParam
                    @NotNull
                    City city,
            @Parameter(description = "Vehicle type", required = true, example = "BIKE")
                    @RequestParam
                    @NotNull
                    VehicleType vehicleType,
            @Parameter(
                            description =
                                    "ISO-8601 instant (optional; defaults to now; must not be in"
                                            + " the future)",
                            example = "2024-01-15T12:00:00Z")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    @PastOrPresent
                    Instant datetime) {
        var at = datetime == null ? Instant.now() : datetime;
        return deliveryFeeService.calculate(city, vehicleType, at);
    }
    // endregion
}
