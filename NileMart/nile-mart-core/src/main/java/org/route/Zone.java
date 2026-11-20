package org.route;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
public enum Zone {
    GREATER_CAIRO(new BigDecimal("50.00"), true),
    ALEX_DELTA(new BigDecimal("65.00"), true),
    CANAL(new BigDecimal("70.00"), true),
    UPPER_EGYPT(new BigDecimal("85.00"), true),
    REMOTE(new BigDecimal("120.00"), false);

    private final BigDecimal fee;
    private final boolean freeShippingEligible;

    public BigDecimal getFee() {
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
