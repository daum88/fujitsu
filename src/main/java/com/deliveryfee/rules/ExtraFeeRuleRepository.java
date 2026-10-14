package com.deliveryfee.rules;

import com.deliveryfee.common.ConditionType;
import com.deliveryfee.common.VehicleType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link ExtraFeeRule} using derived queries and parameterized JPQL only. */
public interface ExtraFeeRuleRepository extends JpaRepository<ExtraFeeRule, Long> {

    /**
     * Returns the extra fee rules active at the given instant for a vehicle and condition, newest
     * first.
     *
     * @param vehicleType vehicle type
     * @param conditionType weather condition
     * @param at calculation instant
     * @return active rules ordered by {@code validFrom} descending
     */
    @Query(
            """
            select r from ExtraFeeRule r
            where r.vehicleType = :vehicleType and r.conditionType = :conditionType
              and r.validFrom <= :at and (r.validTo is null or r.validTo > :at)
            order by r.validFrom desc
            """)
    List<ExtraFeeRule> findActiveRules(
            @Param("vehicleType") VehicleType vehicleType,
            @Param("conditionType") ConditionType conditionType,
            @Param("at") Instant at);

    /** Returns all extra fee rules for a condition and vehicle (used for overlap checks). */
    List<ExtraFeeRule> findAllByConditionTypeAndVehicleType(
            ConditionType conditionType, VehicleType vehicleType);
}
