package com.deliveryfee.rules;

import static com.deliveryfee.TestFixtures.baseRule;
import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.phenomenonRule;
import static com.deliveryfee.TestFixtures.temperatureRule;
import static com.deliveryfee.common.City.TALLINN;
import static com.deliveryfee.common.City.TARTU;
import static com.deliveryfee.common.ConditionType.PHENOMENON;
import static com.deliveryfee.common.ConditionType.TEMPERATURE;
import static com.deliveryfee.common.PhenomenonCategory.RAIN;
import static com.deliveryfee.common.PhenomenonCategory.SNOW_SLEET;
import static com.deliveryfee.common.VehicleType.BIKE;
import static com.deliveryfee.common.VehicleType.CAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deliveryfee.TestFixtures;
import com.deliveryfee.common.ConflictException;
import com.deliveryfee.common.NotFoundException;
import com.deliveryfee.rules.dto.BaseFeeRuleRequest;
import com.deliveryfee.rules.dto.BaseFeeRuleResponse;
import com.deliveryfee.rules.dto.ExtraFeeRuleRequest;
import com.deliveryfee.rules.dto.ExtraFeeRuleResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class FeeRuleServiceTest {

    private static final Instant FROM_1990 = Instant.parse("1990-01-01T00:00:00Z");
    private static final Instant TO_1995 = Instant.parse("1995-01-01T00:00:00Z");
    private static final Instant FROM_2010 = Instant.parse("2010-01-01T00:00:00Z");

    @Mock private BaseFeeRuleRepository baseFeeRuleRepository;

    @Mock private ExtraFeeRuleRepository extraFeeRuleRepository;

    private FeeRuleService service;

    @BeforeEach
    void setUp() {
        service = new FeeRuleService(baseFeeRuleRepository, extraFeeRuleRepository);
    }

    @Test
    void createBaseFee_overlappingOpenEndedRule_throwsConflict() {
        when(baseFeeRuleRepository.findAllByCityAndVehicleType(TALLINN, CAR))
                .thenReturn(List.of(baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null)));

        BaseFeeRuleRequest request =
                new BaseFeeRuleRequest(TALLINN, CAR, bd("5.00"), FROM_2010, null);

        assertThatThrownBy(() -> service.createBaseFee(request))
                .isInstanceOf(ConflictException.class);
        verify(baseFeeRuleRepository, never()).save(any());
    }

    @Test
    void createBaseFee_nonOverlappingHistoricalWindow_returnsMappedRule() {
        when(baseFeeRuleRepository.findAllByCityAndVehicleType(TALLINN, CAR)).thenReturn(List.of());
        BaseFeeRule saved = baseRule(TALLINN, CAR, "5.00", FROM_1990, TO_1995);
        when(baseFeeRuleRepository.save(any(BaseFeeRule.class))).thenReturn(saved);

        BaseFeeRuleRequest request =
                new BaseFeeRuleRequest(TALLINN, CAR, bd("5.00"), FROM_1990, TO_1995);

        BaseFeeRuleResponse response = service.createBaseFee(request);

        assertThat(response.city()).isEqualTo(TALLINN);
        assertThat(response.vehicleType()).isEqualTo(CAR);
        assertThat(response.fee()).isEqualByComparingTo("5.00");
    }

    @Test
    void updateBaseFee_unknownId_throwsNotFound() {
        when(baseFeeRuleRepository.findById(42L)).thenReturn(Optional.empty());

        BaseFeeRuleRequest request =
                new BaseFeeRuleRequest(TALLINN, CAR, bd("5.00"), TestFixtures.EPOCH, null);

        assertThatThrownBy(() -> service.updateBaseFee(42L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteBaseFee_unknownId_throwsNotFound() {
        when(baseFeeRuleRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteBaseFee(42L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createExtraFee_samePhenomenonCategoryAndOverlappingWindow_throwsConflict() {
        when(extraFeeRuleRepository.findAllByConditionTypeAndVehicleType(PHENOMENON, BIKE))
                .thenReturn(List.of(phenomenonRule(BIKE, SNOW_SLEET, "1.00", false)));

        ExtraFeeRuleRequest request =
                new ExtraFeeRuleRequest(
                        PHENOMENON,
                        BIKE,
                        null,
                        null,
                        null,
                        null,
                        SNOW_SLEET,
                        bd("1.50"),
                        false,
                        FROM_2010,
                        null);

        assertThatThrownBy(() -> service.createExtraFee(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createExtraFee_differentPhenomenonCategory_doesNotConflict() {
        when(extraFeeRuleRepository.findAllByConditionTypeAndVehicleType(PHENOMENON, BIKE))
                .thenReturn(List.of(phenomenonRule(BIKE, SNOW_SLEET, "1.00", false)));
        ExtraFeeRule saved = phenomenonRule(BIKE, RAIN, "1.50", false);
        when(extraFeeRuleRepository.save(any(ExtraFeeRule.class))).thenReturn(saved);

        ExtraFeeRuleRequest request =
                new ExtraFeeRuleRequest(
                        PHENOMENON,
                        BIKE,
                        null,
                        null,
                        null,
                        null,
                        RAIN,
                        bd("1.50"),
                        false,
                        FROM_2010,
                        null);

        assertThat(service.createExtraFee(request).phenomenonCategory()).isEqualTo(RAIN);
    }

    @Test
    void listBaseFees_returnsMappedRules() {
        when(baseFeeRuleRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null)));

        List<BaseFeeRuleResponse> result = service.listBaseFees();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fee()).isEqualByComparingTo("4.00");
    }

    @Test
    void updateBaseFee_success_updatesAndReturnsRule() {
        BaseFeeRule existing = baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null);
        when(baseFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(baseFeeRuleRepository.findAllByCityAndVehicleType(TARTU, CAR)).thenReturn(List.of());

        BaseFeeRuleRequest request =
                new BaseFeeRuleRequest(TARTU, CAR, bd("3.50"), FROM_1990, TO_1995);

        BaseFeeRuleResponse response = service.updateBaseFee(1L, request);

        assertThat(response.city()).isEqualTo(TARTU);
        assertThat(existing.getFee()).isEqualByComparingTo("3.50");
    }

    @Test
    void deleteBaseFee_success_deletesRule() {
        BaseFeeRule existing = baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null);
        when(baseFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.deleteBaseFee(1L);

        verify(baseFeeRuleRepository).delete(existing);
    }

    @Test
    void listExtraFees_returnsMappedRules() {
        when(extraFeeRuleRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(phenomenonRule(BIKE, RAIN, "0.50", false)));

        List<ExtraFeeRuleResponse> result = service.listExtraFees();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).phenomenonCategory()).isEqualTo(RAIN);
    }

    @Test
    void updateExtraFee_unknownId_throwsNotFound() {
        when(extraFeeRuleRepository.findById(42L)).thenReturn(Optional.empty());

        ExtraFeeRuleRequest request =
                new ExtraFeeRuleRequest(
                        PHENOMENON,
                        BIKE,
                        null,
                        null,
                        null,
                        null,
                        RAIN,
                        bd("0.50"),
                        false,
                        FROM_2010,
                        null);

        assertThatThrownBy(() -> service.updateExtraFee(42L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateExtraFee_success_updatesAndReturnsRule() {
        ExtraFeeRule existing = phenomenonRule(BIKE, RAIN, "0.50", false);
        when(extraFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(extraFeeRuleRepository.findAllByConditionTypeAndVehicleType(PHENOMENON, BIKE))
                .thenReturn(List.of());

        ExtraFeeRuleRequest request =
                new ExtraFeeRuleRequest(
                        PHENOMENON,
                        BIKE,
                        null,
                        null,
                        null,
                        null,
                        RAIN,
                        bd("1.00"),
                        false,
                        FROM_2010,
                        null);

        ExtraFeeRuleResponse response = service.updateExtraFee(1L, request);

        assertThat(response.fee()).isEqualByComparingTo("1.00");
        assertThat(existing.getFee()).isEqualByComparingTo("1.00");
    }

    @Test
    void deleteExtraFee_unknownId_throwsNotFound() {
        when(extraFeeRuleRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteExtraFee(42L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteExtraFee_success_deletesRule() {
        ExtraFeeRule existing = phenomenonRule(BIKE, RAIN, "0.50", false);
        when(extraFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.deleteExtraFee(1L);

        verify(extraFeeRuleRepository).delete(existing);
    }

    @Test
    void createExtraFee_numericOverlappingRange_throwsConflict() {
        ExtraFeeRule existing = temperatureRule(BIKE, bd("-10"), true, bd("0"), true, "0.50");
        when(extraFeeRuleRepository.findAllByConditionTypeAndVehicleType(TEMPERATURE, BIKE))
                .thenReturn(List.of(existing));

        ExtraFeeRuleRequest request =
                new ExtraFeeRuleRequest(
                        TEMPERATURE,
                        BIKE,
                        bd("-5"),
                        true,
                        bd("5"),
                        true,
                        null,
                        bd("0.50"),
                        false,
                        FROM_2010,
                        null);

        assertThatThrownBy(() -> service.createExtraFee(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateBaseFee_overlappingWindow_throwsConflict() {
        BaseFeeRule existing = baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null);
        when(baseFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(baseFeeRuleRepository.findAllByCityAndVehicleType(TARTU, CAR))
                .thenReturn(List.of(baseRule(TARTU, CAR, "3.00", TestFixtures.EPOCH, null)));

        BaseFeeRuleRequest request =
                new BaseFeeRuleRequest(TARTU, CAR, bd("3.50"), FROM_2010, null);

        assertThatThrownBy(() -> service.updateBaseFee(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateBaseFee_keepingItsOwnWindow_doesNotConflict() {
        BaseFeeRule existing = baseRule(TALLINN, CAR, "4.00", TestFixtures.EPOCH, null);
        existing.setId(1L);
        when(baseFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(baseFeeRuleRepository.findAllByCityAndVehicleType(TALLINN, CAR))
                .thenReturn(List.of(existing));

        BaseFeeRuleRequest request =
                new BaseFeeRuleRequest(TALLINN, CAR, bd("4.50"), TestFixtures.EPOCH, null);

        BaseFeeRuleResponse response = service.updateBaseFee(1L, request);

        assertThat(response.fee()).isEqualByComparingTo("4.50");
    }

    @Test
    void updateExtraFee_overlappingWindow_throwsConflict() {
        ExtraFeeRule existing = phenomenonRule(BIKE, RAIN, "0.50", false);
        when(extraFeeRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(extraFeeRuleRepository.findAllByConditionTypeAndVehicleType(PHENOMENON, BIKE))
                .thenReturn(List.of(phenomenonRule(BIKE, RAIN, "1.00", false)));

        ExtraFeeRuleRequest request =
                new ExtraFeeRuleRequest(
                        PHENOMENON,
                        BIKE,
                        null,
                        null,
                        null,
                        null,
                        RAIN,
                        bd("1.50"),
                        false,
                        FROM_2010,
                        null);

        assertThatThrownBy(() -> service.updateExtraFee(1L, request))
                .isInstanceOf(ConflictException.class);
    }
}
