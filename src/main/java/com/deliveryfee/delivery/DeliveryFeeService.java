package com.deliveryfee.delivery;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.delivery.dto.DeliveryFeeResponse;
import com.deliveryfee.delivery.dto.FeeBreakdown;
import com.deliveryfee.weather.WeatherObservation;
import com.deliveryfee.weather.WeatherObservationService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates a fee calculation: resolve weather, run the engine, map to the response DTO. */
@Service
@RequiredArgsConstructor
public class DeliveryFeeService {

    private final WeatherObservationService weatherObservationService;
    private final DeliveryFeeCalculator calculator;

    /**
     * Calculates the delivery fee for the given city, vehicle and point in time.
     *
     * @param city city of delivery (determines station + regional rules)
     * @param vehicleType vehicle used for delivery
     * @param at calculation instant (must not be in the future)
     * @return the full fee response, never {@code null}
     */
    @Transactional(readOnly = true)
    public DeliveryFeeResponse calculate(City city, VehicleType vehicleType, Instant at) {
        WeatherObservation observation = weatherObservationService.latestFor(city, at);
        FeeBreakdown breakdown =
                calculator.calculate(new DeliveryContext(city, vehicleType, at, observation));
        return DeliveryFeeResponse.of(
                city, vehicleType, breakdown, at, observation.getObservedAt());
    }
}
