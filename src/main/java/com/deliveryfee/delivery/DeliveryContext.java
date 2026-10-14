package com.deliveryfee.delivery;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.weather.WeatherObservation;
import java.time.Instant;

/**
 * Everything a fee calculation needs: the delivery city and vehicle, the requested instant, and the
 * resolved weather observation.
 */
public record DeliveryContext(
        City city, VehicleType vehicle, Instant requestedAt, WeatherObservation observation) {}
