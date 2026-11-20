package org.route;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PromotionalPricingService implements PricingService {
    @Override
    public BigDecimal getPrice(Product product, int qty) {
        BigDecimal rawTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
        return rawTotal.multiply(new BigDecimal("0.80"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
