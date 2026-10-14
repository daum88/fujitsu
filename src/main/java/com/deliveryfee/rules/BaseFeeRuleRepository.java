package com.deliveryfee.rules;

import com.deliveryfee.common.City;
import com.deliveryfee.common.VehicleType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link BaseFeeRule} using derived queries and parameterized JPQL only. */
public interface BaseFeeRuleRepository extends JpaRepository<BaseFeeRule, Long> {

    /**
     * Returns the base fee rules active at the given instant for a city and vehicle, newest first.
     *
     * @param city delivery city
     * @param vehicleType vehicle type
     * @param at calculation instant
     * @return active rules ordered by {@code validFrom} descending
     */
    @Query(
            """
            select r from BaseFeeRule r
            where r.city = :city and r.vehicleType = :vehicleType
              and r.validFrom <= :at and (r.validTo is null or r.validTo > :at)
            order by r.validFrom desc
            """)
    List<BaseFeeRule> findActiveRules(
            @Param("city") City city,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("at") Instant at);

    /** Returns all base fee rules for a city and vehicle (used for overlap checks). */
    List<BaseFeeRule> findAllByCityAndVehicleType(City city, VehicleType vehicleType);
}
