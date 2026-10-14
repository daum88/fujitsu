package com.deliveryfee.rules;

import static com.deliveryfee.TestFixtures.baseRule;
import static com.deliveryfee.TestFixtures.bd;
import static com.deliveryfee.TestFixtures.phenomenonRule;
import static com.deliveryfee.common.City.TALLINN;
import static com.deliveryfee.common.ConditionType.PHENOMENON;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
