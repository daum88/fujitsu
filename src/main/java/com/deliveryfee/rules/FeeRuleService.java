package com.deliveryfee.rules;

import com.deliveryfee.common.ConflictException;
import com.deliveryfee.common.IntervalUtils;
import com.deliveryfee.common.NotFoundException;
import com.deliveryfee.rules.dto.BaseFeeRuleRequest;
import com.deliveryfee.rules.dto.BaseFeeRuleResponse;
import com.deliveryfee.rules.dto.ExtraFeeRuleRequest;
import com.deliveryfee.rules.dto.ExtraFeeRuleResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD + validity orchestration for both rule types. Creating or updating a rule whose validity
 * window overlaps an existing rule (with the same key and condition) is rejected with a conflict.
 */
@Service
@RequiredArgsConstructor
public class FeeRuleService {

    private final BaseFeeRuleRepository baseFeeRuleRepository;
    private final ExtraFeeRuleRepository extraFeeRuleRepository;

    /** Lists all base fee rules. */
    @Transactional(readOnly = true)
    public List<BaseFeeRuleResponse> listBaseFees() {
        return baseFeeRuleRepository.findAll().stream().map(BaseFeeRuleResponse::from).toList();
    }

    /** Creates a base fee rule after verifying there is no overlapping active window. */
    @Transactional
    public BaseFeeRuleResponse createBaseFee(BaseFeeRuleRequest request) {
        ensureNoBaseFeeOverlap(request, null);
        BaseFeeRule rule =
                new BaseFeeRule(
                        request.city(),
                        request.vehicleType(),
                        request.fee(),
                        request.validFrom(),
                        request.validTo());
        return BaseFeeRuleResponse.from(baseFeeRuleRepository.save(rule));
    }

    /** Updates a base fee rule after verifying there is no overlapping active window. */
    @Transactional
    public BaseFeeRuleResponse updateBaseFee(Long id, BaseFeeRuleRequest request) {
        BaseFeeRule existing =
                baseFeeRuleRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Base fee rule", id));
        ensureNoBaseFeeOverlap(request, id);
        existing.setCity(request.city());
        existing.setVehicleType(request.vehicleType());
        existing.setFee(request.fee());
        existing.setValidFrom(request.validFrom());
        existing.setValidTo(request.validTo());
        return BaseFeeRuleResponse.from(existing);
    }

    /** Deletes a base fee rule; historical calculations stay reproducible via validity windows. */
    @Transactional
    public void deleteBaseFee(Long id) {
        BaseFeeRule existing =
                baseFeeRuleRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Base fee rule", id));
        baseFeeRuleRepository.delete(existing);
    }

    /** Lists all extra fee rules. */
    @Transactional(readOnly = true)
    public List<ExtraFeeRuleResponse> listExtraFees() {
        return extraFeeRuleRepository.findAll().stream().map(ExtraFeeRuleResponse::from).toList();
    }

    /** Creates an extra fee rule after verifying there is no conflicting overlapping rule. */
    @Transactional
    public ExtraFeeRuleResponse createExtraFee(ExtraFeeRuleRequest request) {
        ensureNoExtraFeeOverlap(request, null);
        return ExtraFeeRuleResponse.from(extraFeeRuleRepository.save(toEntity(request)));
    }

    /** Updates an extra fee rule after verifying there is no conflicting overlapping rule. */
    @Transactional
    public ExtraFeeRuleResponse updateExtraFee(Long id, ExtraFeeRuleRequest request) {
        ExtraFeeRule existing =
                extraFeeRuleRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Extra fee rule", id));
        ensureNoExtraFeeOverlap(request, id);
        applyRequest(existing, request);
        return ExtraFeeRuleResponse.from(existing);
    }

    /**
     * Deletes an extra fee rule; historical calculations stay reproducible via validity windows.
     */
    @Transactional
    public void deleteExtraFee(Long id) {
        ExtraFeeRule existing =
                extraFeeRuleRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("Extra fee rule", id));
        extraFeeRuleRepository.delete(existing);
    }

    private void ensureNoBaseFeeOverlap(BaseFeeRuleRequest request, Long excludeId) {
        boolean overlaps =
                baseFeeRuleRepository
                        .findAllByCityAndVehicleType(request.city(), request.vehicleType())
                        .stream()
                        .filter(rule -> excludeId == null || !rule.getId().equals(excludeId))
                        .anyMatch(
                                rule ->
                                        IntervalUtils.overlaps(
                                                rule.getValidFrom(),
                                                rule.getValidTo(),
                                                request.validFrom(),
                                                request.validTo()));
        if (overlaps) {
            throw new ConflictException(
                    "Overlapping base fee rule for "
                            + request.city()
                            + "/"
                            + request.vehicleType());
        }
    }

    private void ensureNoExtraFeeOverlap(ExtraFeeRuleRequest request, Long excludeId) {
        ExtraFeeRule candidate = toEntity(request);
        boolean overlaps =
                extraFeeRuleRepository
                        .findAllByConditionTypeAndVehicleType(
                                request.conditionType(), request.vehicleType())
                        .stream()
                        .filter(rule -> excludeId == null || !rule.getId().equals(excludeId))
                        .filter(
                                rule ->
                                        IntervalUtils.overlaps(
                                                rule.getValidFrom(),
                                                rule.getValidTo(),
                                                request.validFrom(),
                                                request.validTo()))
                        .anyMatch(candidate::overlapsCondition);
        if (overlaps) {
            throw new ConflictException(
                    "Overlapping extra fee rule for "
                            + request.conditionType()
                            + "/"
                            + request.vehicleType());
        }
    }

    private static ExtraFeeRule toEntity(ExtraFeeRuleRequest request) {
        return new ExtraFeeRule(
                request.conditionType(),
                request.vehicleType(),
                request.minValue(),
                request.minInclusive(),
                request.maxValue(),
                request.maxInclusive(),
                request.phenomenonCategory(),
                request.fee(),
                request.forbidden(),
                request.validFrom(),
                request.validTo());
    }

    private static void applyRequest(ExtraFeeRule existing, ExtraFeeRuleRequest request) {
        existing.setConditionType(request.conditionType());
        existing.setVehicleType(request.vehicleType());
        existing.setMinValue(request.minValue());
        existing.setMinInclusive(request.minInclusive());
        existing.setMaxValue(request.maxValue());
        existing.setMaxInclusive(request.maxInclusive());
        existing.setPhenomenonCategory(request.phenomenonCategory());
        existing.setFee(request.fee());
        existing.setForbidden(request.forbidden());
        existing.setValidFrom(request.validFrom());
        existing.setValidTo(request.validTo());
    }
}
