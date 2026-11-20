package org.route;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class ShoppingCart {

    private final ProductService productService;
    private final PricingService pricingService;
    private final int maxQuantityPerProduct;

    @Getter
    @Setter
    private int cartNumber;

    @Getter
    @Setter
    private Customer customer;

    private final Map<String, Integer> lines = new LinkedHashMap<>();

    public ShoppingCart(
            ProductService productService,
            PricingService pricingService,
            @Value("${store.max-quantity-per-product}") int maxQuantityPerProduct) {
        this.productService = productService;
        this.pricingService = pricingService;
        this.maxQuantityPerProduct = maxQuantityPerProduct;
    }

    public void addItem(String sku, int qty) {
        if (qty <= 0) {
            throw new StoreException("Quantity must be at least 1.");
        }

        Product product = productService.findBySku(sku);

        // CART-01
        if (product.getStockQuantity() == 0) {
            throw new StoreException("Product '" + product.getName() + "' is out of stock.");
        }

        // CART-02 & CART-03
        int currentQty = lines.getOrDefault(sku, 0);
        int newQty = currentQty + qty;

        if (newQty > maxQuantityPerProduct) {
            throw new StoreException("Cannot add more than " + maxQuantityPerProduct
                    + " units of '" + product.getName() + "' per cart.");
        }

        if (newQty > product.getStockQuantity()) {
            throw new StoreException("Cannot add " + newQty + " units of '" + product.getName()
                    + "'. Only " + product.getStockQuantity() + " available.");
        }

        lines.put(sku, newQty);
    }

    public BigDecimal subtotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> entry : lines.entrySet()) {
            Product product = productService.findBySku(entry.getKey());
            sum = sum.add(pricingService.getPrice(product, entry.getValue()));
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Integer> getLines() {
        return Collections.unmodifiableMap(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public void clear() {
        lines.clear();
    }
}
