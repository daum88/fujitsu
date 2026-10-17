package com.deliveryfee.delivery;

import static com.deliveryfee.TestFixtures.baseRule;
import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.observation;
import static com.deliveryfee.common.City.TARTU;
import static com.deliveryfee.common.VehicleType.BIKE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveryfee.rules.BaseFeeRule;
import com.deliveryfee.rules.BaseFeeRuleRepository;
import com.deliveryfee.rules.FeeRuleService;
import com.deliveryfee.weather.WeatherObservationRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeliveryFeeIntegrationTest {

    private static final Instant OBS_2024 = Instant.parse("2024-06-01T10:00:00Z");
    private static final Instant OBS_2025 = Instant.parse("2025-06-01T10:00:00Z");

    @Autowired private MockMvc mockMvc;

    @Autowired private WeatherObservationRepository observationRepository;

    @Autowired private BaseFeeRuleRepository baseFeeRuleRepository;

    @Autowired private FeeRuleService feeRuleService;

    @Test
    void specExample_tartuBike_returnsFourEuros() throws Exception {
        // The exact worked example from the trial task: TARTU + BIKE, -2.1 C, 4.7 m/s.
        observationRepository.save(
                observation(
                        "Tartu-Tõravere",
                        bd("-2.1"),
                        bd("4.7"),
                        "Light snow shower",
                        Instant.parse("2024-01-15T10:00:00Z")));

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2024-01-15T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(4.0))
                .andExpect(jsonPath("$.breakdown.regionalBaseFee").value(2.5))
                .andExpect(jsonPath("$.breakdown.airTemperatureExtraFee").value(0.5))
                .andExpect(jsonPath("$.breakdown.windSpeedExtraFee").value(0.0))
                .andExpect(jsonPath("$.breakdown.weatherPhenomenonExtraFee").value(1.0));
    }

    @Test
    void historicalDatetime_usesOlderObservationAndOlderRuleVersion() throws Exception {
        observationRepository.save(
                observation(
                        "Tartu-Tõravere", bd("-5.0"), bd("5.0"), "Light snow shower", OBS_2024));
        observationRepository.save(
                observation(
                        "Tartu-Tõravere",
                        bd("15.0"),
                        bd("5.0"),
                        "Cloudy with clear spells",
                        OBS_2025));
        baseFeeRuleRepository.save(
                baseRule(TARTU, BIKE, "3.00", Instant.parse("2025-01-01T00:00:00Z"), null));

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2024-06-01T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(4.0))
                .andExpect(jsonPath("$.breakdown.regionalBaseFee").value(2.5))
                .andExpect(jsonPath("$.weatherObservedAt").value("2024-06-01T10:00:00Z"));

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2025-06-01T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(3.0))
                .andExpect(jsonPath("$.breakdown.regionalBaseFee").value(3.0))
                .andExpect(jsonPath("$.weatherObservedAt").value("2025-06-01T10:00:00Z"));
    }

    @Test
    void deletingAFutureRule_leavesHistoricalCalculationReproducible() throws Exception {
        observationRepository.save(
                observation(
                        "Tartu-Tõravere", bd("-5.0"), bd("5.0"), "Light snow shower", OBS_2024));
        BaseFeeRule futureRule =
                baseFeeRuleRepository.save(
                        baseRule(TARTU, BIKE, "3.00", Instant.parse("2025-01-01T00:00:00Z"), null));

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2024-06-01T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(4.0));

        feeRuleService.deleteBaseFee(futureRule.getId());

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2024-06-01T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFee").value(4.0));
    }

    @Test
    void futureDatetime_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2999-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void datetimeBeforeFirstObservation_returns422() throws Exception {
        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "1990-01-01T00:00:00Z"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void openApiJsonIsGenerated() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/v1/delivery-fee']").exists());
    }
}
