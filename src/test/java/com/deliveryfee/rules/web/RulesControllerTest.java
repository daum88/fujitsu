package com.deliveryfee.rules.web;

import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.common.City.TALLINN;
import static com.deliveryfee.common.ConditionType.PHENOMENON;
import static com.deliveryfee.common.PhenomenonCategory.RAIN;
import static com.deliveryfee.common.VehicleType.CAR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deliveryfee.common.ConflictException;
import com.deliveryfee.common.NotFoundException;
import com.deliveryfee.rules.FeeRuleService;
import com.deliveryfee.rules.dto.BaseFeeRuleRequest;
import com.deliveryfee.rules.dto.BaseFeeRuleResponse;
import com.deliveryfee.rules.dto.ExtraFeeRuleRequest;
import com.deliveryfee.rules.dto.ExtraFeeRuleResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {BaseFeeRuleController.class, ExtraFeeRuleController.class})
class RulesControllerTest {

    private static final Instant FROM = Instant.parse("2026-10-14T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FeeRuleService feeRuleService;

    @Test
    void createBaseFee_validBody_returns201() throws Exception {
        BaseFeeRuleResponse response =
                new BaseFeeRuleResponse(1L, TALLINN, CAR, bd("4.00"), FROM, null);
        when(feeRuleService.createBaseFee(any(BaseFeeRuleRequest.class))).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/rules/base-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":4.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.city").value("TALLINN"))
                .andExpect(jsonPath("$.fee").value(4.0));
    }

    @Test
    void createBaseFee_negativeFee_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/base-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":-1.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_bothBoundsAndCategory_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","minValue":-10,"minInclusive":true,"maxValue":0,"maxInclusive":true,"phenomenonCategory":"RAIN","fee":0.50,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBaseFee_unknownId_returns404() throws Exception {
        when(feeRuleService.updateBaseFee(eq(42L), any(BaseFeeRuleRequest.class)))
                .thenThrow(new NotFoundException("Base fee rule", 42));

        mockMvc.perform(
                        put("/api/v1/rules/base-fees/42")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":4.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBaseFee_overlappingRule_returns409() throws Exception {
        when(feeRuleService.createBaseFee(any(BaseFeeRuleRequest.class)))
                .thenThrow(new ConflictException("Overlapping base fee rule"));

        mockMvc.perform(
                        post("/api/v1/rules/base-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":4.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteBaseFee_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/rules/base-fees/1")).andExpect(status().isNoContent());
    }

    @Test
    void listBaseFees_returns200() throws Exception {
        when(feeRuleService.listBaseFees()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/rules/base-fees")).andExpect(status().isOk());
    }

    @Test
    void updateBaseFee_validBody_returns200() throws Exception {
        BaseFeeRuleResponse response =
                new BaseFeeRuleResponse(1L, TALLINN, CAR, bd("4.00"), FROM, null);
        when(feeRuleService.updateBaseFee(eq(1L), any(BaseFeeRuleRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/v1/rules/base-fees/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":4.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void listExtraFees_returns200() throws Exception {
        when(feeRuleService.listExtraFees()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/rules/extra-fees")).andExpect(status().isOk());
    }

    @Test
    void createExtraFee_validPhenomenonBody_returns201() throws Exception {
        ExtraFeeRuleResponse response =
                new ExtraFeeRuleResponse(
                        1L,
                        PHENOMENON,
                        CAR,
                        null,
                        null,
                        null,
                        null,
                        RAIN,
                        bd("0.50"),
                        false,
                        FROM,
                        null);
        when(feeRuleService.createExtraFee(any(ExtraFeeRuleRequest.class))).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"PHENOMENON","vehicleType":"CAR","phenomenonCategory":"RAIN","fee":0.50,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateExtraFee_validBody_returns200() throws Exception {
        ExtraFeeRuleResponse response =
                new ExtraFeeRuleResponse(
                        1L,
                        PHENOMENON,
                        CAR,
                        null,
                        null,
                        null,
                        null,
                        RAIN,
                        bd("0.50"),
                        false,
                        FROM,
                        null);
        when(feeRuleService.updateExtraFee(eq(1L), any(ExtraFeeRuleRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/v1/rules/extra-fees/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"PHENOMENON","vehicleType":"CAR","phenomenonCategory":"RAIN","fee":0.50,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteExtraFee_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/rules/extra-fees/1")).andExpect(status().isNoContent());
    }

    @Test
    void createExtraFee_minValueWithoutInclusiveFlag_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","minValue":-10,"fee":1.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_missingFeeWhenNotForbidden_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","minValue":-10,"minInclusive":true,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_maxValueWithoutInclusiveFlag_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","maxValue":0,"fee":1.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_minValueAboveMaxValue_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","minValue":20,"minInclusive":true,"maxValue":10,"maxInclusive":true,"fee":1.00,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBaseFee_validToBeforeValidFrom_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/base-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":4.00,"validFrom":"2020-01-01T00:00:00Z","validTo":"2019-01-01T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBaseFee_malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/base-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ this is not valid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBaseFee_missingValidFromWithValidTo_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/base-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"city":"TALLINN","vehicleType":"CAR","fee":4.00,"validTo":"2020-01-01T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_missingValidFromWithValidTo_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","minValue":-10,"minInclusive":true,"fee":1.00,"validTo":"2020-01-01T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_temperatureWithPhenomenonCategory_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"TEMPERATURE","vehicleType":"BIKE","phenomenonCategory":"RAIN","fee":0.50,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExtraFee_phenomenonWithNumericBounds_returns400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/rules/extra-fees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"conditionType":"PHENOMENON","vehicleType":"BIKE","minValue":-10,"minInclusive":true,"fee":0.50,"validFrom":"2026-10-14T00:00:00Z"}
                                        """))
                .andExpect(status().isBadRequest());
    }
}
