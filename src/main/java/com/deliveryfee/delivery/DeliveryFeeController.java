package com.deliveryfee.delivery;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.delivery.dto.DeliveryFeeResponse;
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
@RestController
@RequestMapping("/api/v1/delivery-fee")
@RequiredArgsConstructor
@Validated
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    /**
     * Calculates the delivery fee.
     *
     * @param city delivery city (strict, case-sensitive enum value)
     * @param vehicleType vehicle type (strict, case-sensitive enum value)
     * @param datetime optional ISO-8601 instant; defaults to now, must not be in the future
     * @return the fee response
     */
    @GetMapping
    public DeliveryFeeResponse calculate(
            @RequestParam @NotNull City city,
            @RequestParam @NotNull VehicleType vehicleType,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    @PastOrPresent
                    Instant datetime) {
        Instant at = datetime == null ? Instant.now() : datetime;
        return deliveryFeeService.calculate(city, vehicleType, at);
    }
}
