package com.deliveryfee.rules;

import com.deliveryfee.common.City;
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

/** The regional base fee (RBF) for a city and vehicle type, valid over a time window. */
@Entity
@Table(name = "base_fee_rule")
@Getter
@Setter
@NoArgsConstructor
public class BaseFeeRule extends AbstractFeeRule {

    @Enumerated(EnumType.STRING)
    @Column(name = "city", nullable = false, length = 20)
    private City city;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    /**
     * Creates a base fee rule.
     *
     * @param city delivery city
     * @param vehicleType vehicle type
     * @param fee regional base fee in EUR
     * @param validFrom start of the validity window (inclusive)
     * @param validTo end of the validity window (exclusive, {@code null} = open-ended)
     */
    public BaseFeeRule(
            City city,
            VehicleType vehicleType,
            BigDecimal fee,
            Instant validFrom,
            Instant validTo) {
        super(validFrom, validTo);
        this.city = city;
        this.vehicleType = vehicleType;
        this.fee = fee;
    }
}
