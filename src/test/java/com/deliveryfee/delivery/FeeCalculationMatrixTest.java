package com.deliveryfee.delivery;

import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.observation;
import static com.deliveryfee.common.City.PÄRNU;
import static com.deliveryfee.common.City.TALLINN;
import static com.deliveryfee.common.City.TARTU;
import static com.deliveryfee.common.VehicleType.BIKE;
import static com.deliveryfee.common.VehicleType.CAR;
import static com.deliveryfee.common.VehicleType.SCOOTER;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveryfee.common.BusinessMessages;
import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import com.deliveryfee.weather.WeatherObservationRepository;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The expected-outcome matrix for the whole fee schedule. Every scenario pins the exact total fee
 * the spec requires, so a change to the engine, the matchers or the seeded rules fails loudly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeeCalculationMatrixTest {

    private static final Instant OBSERVED_AT = Instant.parse("2024-03-15T10:00:00Z");
    private static final String QUERY_AT = "2024-03-15T11:00:00Z";

    @Autowired private MockMvc mockMvc;

    @Autowired private WeatherObservationRepository observationRepository;

    @ParameterizedTest(name = "{0} {1} | temp={2} wind={3} phenomenon=\"{4}\" => {5}")
    @MethodSource("expectedFees")
    void expectedFee_matrix(
            City city,
            VehicleType vehicle,
            String temperature,
            String wind,
            String phenomenon,
            String expectedTotal)
            throws Exception {
        seedObservation(city, temperature, wind, phenomenon);

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", city.name())
                                .param("vehicleType", vehicle.name())
                                .param("datetime", QUERY_AT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(Double.parseDouble(expectedTotal)));
    }

    @ParameterizedTest(name = "{0} {1} | temp={2} wind={3} phenomenon=\"{4}\" => forbidden")
    @MethodSource("forbiddenScenarios")
    void forbiddenConditions_return400WithVerbatimMessage(
            City city, VehicleType vehicle, String temperature, String wind, String phenomenon)
            throws Exception {
        seedObservation(city, temperature, wind, phenomenon);

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", city.name())
                                .param("vehicleType", vehicle.name())
                                .param("datetime", QUERY_AT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(BusinessMessages.FORBIDDEN_VEHICLE));
    }

    private void seedObservation(City city, String temperature, String wind, String phenomenon) {
        observationRepository.save(
                observation(
                        city.getStationName(), bd(temperature), bd(wind), phenomenon, OBSERVED_AT));
    }

    private static Stream<Arguments> expectedFees() {
        return Stream.of(
                // Mild weather — regional base fee only (the whole 3x3 base fee schedule)
                arguments(TALLINN, CAR, "5.0", "5.0", "Cloudy with clear spells", "4.00"),
                arguments(TALLINN, SCOOTER, "5.0", "5.0", "Cloudy with clear spells", "3.50"),
                arguments(TALLINN, BIKE, "5.0", "5.0", "Cloudy with clear spells", "3.00"),
                arguments(TARTU, CAR, "5.0", "5.0", "Cloudy with clear spells", "3.50"),
                arguments(TARTU, SCOOTER, "5.0", "5.0", "Cloudy with clear spells", "3.00"),
                arguments(TARTU, BIKE, "5.0", "5.0", "Cloudy with clear spells", "2.50"),
                arguments(PÄRNU, CAR, "5.0", "5.0", "Cloudy with clear spells", "3.00"),
                arguments(PÄRNU, SCOOTER, "5.0", "5.0", "Cloudy with clear spells", "2.50"),
                arguments(PÄRNU, BIKE, "5.0", "5.0", "Cloudy with clear spells", "2.00"),
                // Cold + snow: -5 => +0.50 temperature, snow => +1.00
                arguments(TALLINN, SCOOTER, "-5.0", "5.0", "Light snow shower", "5.00"),
                arguments(TALLINN, BIKE, "-5.0", "5.0", "Light snow shower", "4.50"),
                arguments(TARTU, BIKE, "-5.0", "5.0", "Light snow shower", "4.00"),
                // Extreme cold: -12 => +1.00 temperature
                arguments(TARTU, BIKE, "-12.0", "4.0", "Snow", "4.50"),
                // Rain + wind: bike pays wind (10-20) and rain; scooter pays only rain; car pays
                // none
                arguments(PÄRNU, BIKE, "5.0", "15.0", "Moderate rain", "3.00"),
                arguments(PÄRNU, SCOOTER, "5.0", "15.0", "Moderate rain", "3.00"),
                arguments(TALLINN, CAR, "5.0", "15.0", "Moderate rain", "4.00"));
    }

    private static Stream<Arguments> forbiddenScenarios() {
        return Stream.of(
                // Wind > 20 m/s forbids bikes
                arguments(TALLINN, BIKE, "20.0", "22.0", "Cloudy with clear spells"),
                arguments(PÄRNU, BIKE, "5.0", "22.0", "Cloudy with clear spells"),
                // Thunder / glaze / hail forbids scooters and bikes
                arguments(TALLINN, SCOOTER, "20.0", "5.0", "Thunderstorm"),
                arguments(PÄRNU, SCOOTER, "5.0", "5.0", "Glaze"));
    }
}
