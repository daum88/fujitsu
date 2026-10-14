package com.deliveryfee.rules;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared base for time-aware fee rules.
 *
 * <p>Validity windows are half-open {@code [validFrom, validTo)}: the start is inclusive and the
 * end is exclusive. A {@code null} {@code validTo} means the rule is open-ended.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    protected AbstractFeeRule(Instant validFrom, Instant validTo) {
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    /**
     * Checks whether this rule is active at the given instant using half-open semantics.
     *
     * @param at the instant to check
     * @return {@code true} when {@code validFrom <= at < validTo} (or {@code at >= validFrom} when
     *     open-ended)
     */
    public boolean isActiveAt(Instant at) {
        return !validFrom.isAfter(at) && (validTo == null || validTo.isAfter(at));
    }
}
