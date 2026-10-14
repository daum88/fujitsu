package com.deliveryfee.delivery;

import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.common.City.TARTU;
import static com.deliveryfee.common.VehicleType.BIKE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveryfee.common.BusinessMessages;
import com.deliveryfee.common.ForbiddenVehicleUsageException;
import com.deliveryfee.common.NoWeatherDataException;
import com.deliveryfee.delivery.dto.DeliveryFeeResponse;
import com.deliveryfee.delivery.dto.FeeBreakdown;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryFeeController.class)
class DeliveryFeeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DeliveryFeeService deliveryFeeService;

    @Test
    void calculate_returnsFeeResponseMatchingContract() throws Exception {
        FeeBreakdown breakdown = FeeBreakdown.of(bd("2.50"), bd("0.50"), bd("0.00"), bd("1.00"));
        DeliveryFeeResponse response =
                DeliveryFeeResponse.of(
                        TARTU,
                        BIKE,
                        breakdown,
                        Instant.parse("2026-10-14T12:00:00Z"),
                        Instant.parse("2026-10-14T11:00:00Z"));
        when(deliveryFeeService.calculate(eq(TARTU), eq(BIKE), any())).thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("TARTU"))
                .andExpect(jsonPath("$.vehicleType").value("BIKE"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.totalFee").value(4.0))
                .andExpect(jsonPath("$.breakdown.regionalBaseFee").value(2.5))
                .andExpect(jsonPath("$.breakdown.airTemperatureExtraFee").value(0.5))
                .andExpect(jsonPath("$.breakdown.windSpeedExtraFee").value(0.0))
                .andExpect(jsonPath("$.breakdown.weatherPhenomenonExtraFee").value(1.0));
    }

    @Test
    void calculate_forbiddenVehicle_returns400WithVerbatimMessage() throws Exception {
        when(deliveryFeeService.calculate(any(), any(), any()))
                .thenThrow(new ForbiddenVehicleUsageException(BusinessMessages.FORBIDDEN_VEHICLE));

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value(BusinessMessages.FORBIDDEN_VEHICLE));
    }

    @Test
    void calculate_noWeatherData_returns422() throws Exception {
        when(deliveryFeeService.calculate(any(), any(), any()))
                .thenThrow(
                        new NoWeatherDataException(TARTU, Instant.parse("2026-10-14T12:00:00Z")));

        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void calculate_lowercaseEnum_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "tartu")
                                .param("vehicleType", "BIKE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculate_missingCity_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/delivery-fee").param("vehicleType", "BIKE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculate_malformedDatetime_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculate_futureDatetime_returns400() throws Exception {
        mockMvc.perform(
                        get("/api/v1/delivery-fee")
                                .param("city", "TARTU")
                                .param("vehicleType", "BIKE")
                                .param("datetime", "2999-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }
}
