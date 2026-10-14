package com.deliveryfee.rules;

import com.deliveryfee.common.ConditionType;
import com.deliveryfee.common.PhenomenonCategory;
import com.deliveryfee.common.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A weather extra fee rule. One generic table models all three conditions (temperature, wind,
 * phenomenon) and both outcomes (surcharge / forbidden vehicle). For numeric conditions the bounds
 * are {@code [minValue, maxValue]} with inclusivity flags; for phenomenon conditions the {@link
 * PhenomenonCategory} is set instead.
 */
@Entity
@Table(name = "extra_fee_rule")
@Getter
@Setter
@NoArgsConstructor
public class ExtraFeeRule extends AbstractFeeRule {

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 20)
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "min_value", precision = 4, scale = 1)
    private BigDecimal minValue;

    @Column(name = "min_inclusive")
    private Boolean minInclusive;

    @Column(name = "max_value", precision = 4, scale = 1)
    private BigDecimal maxValue;

    @Column(name = "max_inclusive")
    private Boolean maxInclusive;

    @Enumerated(EnumType.STRING)
    @Column(name = "phenomenon_category", length = 30)
    private PhenomenonCategory phenomenonCategory;

    @Column(name = "fee", precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "forbidden", nullable = false)
    private boolean forbidden;

    /** Creates a new extra fee rule. */
    public ExtraFeeRule(
            ConditionType conditionType,
            VehicleType vehicleType,
            BigDecimal minValue,
            Boolean minInclusive,
            BigDecimal maxValue,
            Boolean maxInclusive,
            PhenomenonCategory phenomenonCategory,
            BigDecimal fee,
            boolean forbidden,
            Instant validFrom,
            Instant validTo) {
        super(validFrom, validTo);
        this.conditionType = conditionType;
        this.vehicleType = vehicleType;
        this.minValue = minValue;
        this.minInclusive = minInclusive;
        this.maxValue = maxValue;
        this.maxInclusive = maxInclusive;
        this.phenomenonCategory = phenomenonCategory;
        this.fee = fee;
        this.forbidden = forbidden;
    }

    /**
     * Checks whether this rule's condition predicate conflicts with another rule's predicate, i.e.
     * whether a single observation could satisfy both.
     *
     * @param other the other extra fee rule (same condition type and vehicle)
     * @return {@code true} when the condition predicates overlap
     */
    public boolean overlapsCondition(ExtraFeeRule other) {
        boolean thisPhenomenon = phenomenonCategory != null;
        boolean otherPhenomenon = other.phenomenonCategory != null;
        if (thisPhenomenon || otherPhenomenon) {
            return thisPhenomenon
                    && otherPhenomenon
                    && phenomenonCategory == other.phenomenonCategory;
        }
        return !rangeEndsBeforeStart(this, other) && !rangeEndsBeforeStart(other, this);
    }

    private static boolean rangeEndsBeforeStart(ExtraFeeRule first, ExtraFeeRule second) {
        if (first.maxValue == null || second.minValue == null) {
            return false;
        }
        int comparison = first.maxValue.compareTo(second.minValue);
        if (comparison < 0) {
            return true;
        }
        if (comparison > 0) {
            return false;
        }
        return !(Boolean.TRUE.equals(first.maxInclusive)
                && Boolean.TRUE.equals(second.minInclusive));
    }
}
