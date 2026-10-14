package com.deliveryfee.delivery;

import static com.deliveryfee.TestFixtures.baseRule;
import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.observation;
import static com.deliveryfee.TestFixtures.phenomenonRule;
import static com.deliveryfee.TestFixtures.temperatureRule;
import static com.deliveryfee.TestFixtures.windRule;
import static com.deliveryfee.common.City.TALLINN;
import static com.deliveryfee.common.City.TARTU;
import static com.deliveryfee.common.ConditionType.PHENOMENON;
import static com.deliveryfee.common.ConditionType.TEMPERATURE;
import static com.deliveryfee.common.ConditionType.WIND;
import static com.deliveryfee.common.PhenomenonCategory.GLAZE_HAIL_THUNDER;
import static com.deliveryfee.common.PhenomenonCategory.RAIN;
import static com.deliveryfee.common.PhenomenonCategory.SNOW_SLEET;
import static com.deliveryfee.common.VehicleType.BIKE;
import static com.deliveryfee.common.VehicleType.CAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.deliveryfee.TestFixtures;
import com.deliveryfee.common.BusinessMessages;
import com.deliveryfee.common.ForbiddenVehicleUsageException;
import com.deliveryfee.delivery.dto.FeeBreakdown;
import com.deliveryfee.rules.BaseFeeRuleRepository;
import com.deliveryfee.rules.ExtraFeeRuleRepository;
import com.deliveryfee.weather.WeatherObservation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryFeeCalculatorTest {

    private static final Instant AT = Instant.parse("2026-10-14T12:00:00Z");

    @Mock private BaseFeeRuleRepository baseFeeRuleRepository;

    @Mock private ExtraFeeRuleRepository extraFeeRuleRepository;

    private DeliveryFeeCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DeliveryFeeCalculator(baseFeeRuleRepository, extraFeeRuleRepository);
    }

    @Test
    void calculate_specExampleTartuBike_returnsFourEuros() {
        when(baseFeeRuleRepository.findActiveRules(TARTU, BIKE, AT))
                .thenReturn(List.of(baseRule(TARTU, BIKE, "2.50", TestFixtures.EPOCH, null)));
        when(extraFeeRuleRepository.findActiveRules(BIKE, TEMPERATURE, AT))
                .thenReturn(List.of(temperatureRule(BIKE, bd("-10"), true, bd("0"), true, "0.50")));
        when(extraFeeRuleRepository.findActiveRules(BIKE, WIND, AT))
                .thenReturn(List.of(windRule(BIKE, bd("10"), true, bd("20"), true, "0.50", false)));
        when(extraFeeRuleRepository.findActiveRules(BIKE, PHENOMENON, AT))
                .thenReturn(
                        List.of(
                                phenomenonRule(BIKE, SNOW_SLEET, "1.00", false),
                                phenomenonRule(BIKE, RAIN, "0.50", false),
                                phenomenonRule(BIKE, GLAZE_HAIL_THUNDER, null, true)));

        DeliveryContext ctx =
                new DeliveryContext(
                        TARTU,
                        BIKE,
                        AT,
                        observation(
                                "Tartu-Tõravere", bd("-5.0"), bd("5.0"), "Light snow shower", AT));

        FeeBreakdown breakdown = calculator.calculate(ctx);

        assertThat(breakdown.total()).isEqualByComparingTo("4.00");
        assertThat(breakdown.regionalBaseFee()).isEqualByComparingTo("2.50");
        assertThat(breakdown.airTemperatureExtraFee()).isEqualByComparingTo("0.50");
        assertThat(breakdown.windSpeedExtraFee()).isEqualByComparingTo("0.00");
        assertThat(breakdown.weatherPhenomenonExtraFee()).isEqualByComparingTo("1.00");
    }

    @Test
    void calculate_carNeverPaysExtras() {
        when(baseFeeRuleRepository.findActiveRules(TALLINN, CAR, AT))
                .thenReturn(List.of(baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null)));
        when(extraFeeRuleRepository.findActiveRules(CAR, TEMPERATURE, AT)).thenReturn(List.of());
        when(extraFeeRuleRepository.findActiveRules(CAR, WIND, AT)).thenReturn(List.of());
        when(extraFeeRuleRepository.findActiveRules(CAR, PHENOMENON, AT)).thenReturn(List.of());

        WeatherObservation extreme =
                observation("Tallinn-Harku", bd("-15.0"), bd("25.0"), "Thunderstorm", AT);
        DeliveryContext ctx = new DeliveryContext(TALLINN, CAR, AT, extreme);

        FeeBreakdown breakdown = calculator.calculate(ctx);

        assertThat(breakdown.total()).isEqualByComparingTo("4.00");
        assertThat(breakdown.airTemperatureExtraFee()).isEqualByComparingTo("0.00");
        assertThat(breakdown.windSpeedExtraFee()).isEqualByComparingTo("0.00");
        assertThat(breakdown.weatherPhenomenonExtraFee()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_forbiddenWindShortCircuitsWithVerbatimMessage() {
        when(baseFeeRuleRepository.findActiveRules(TALLINN, BIKE, AT))
                .thenReturn(List.of(baseRule(TALLINN, BIKE, "3.00", TestFixtures.EPOCH, null)));
        when(extraFeeRuleRepository.findActiveRules(BIKE, TEMPERATURE, AT)).thenReturn(List.of());
        when(extraFeeRuleRepository.findActiveRules(BIKE, WIND, AT))
                .thenReturn(List.of(windRule(BIKE, bd("20"), false, null, null, null, true)));

        WeatherObservation windy = observation("Tallinn-Harku", bd("0"), bd("21.0"), null, AT);
        DeliveryContext ctx = new DeliveryContext(TALLINN, BIKE, AT, windy);

        assertThatThrownBy(() -> calculator.calculate(ctx))
                .isInstanceOf(ForbiddenVehicleUsageException.class)
                .hasMessage(BusinessMessages.FORBIDDEN_VEHICLE);
    }
}
