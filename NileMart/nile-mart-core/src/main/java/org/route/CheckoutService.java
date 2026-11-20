package org.route;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProductService productService;
    private final ShippingQuoteService shippingQuoteService;
    private final TaxCalculator taxCalculator;
    private final OrderRegistry orderRegistry;
    private final List<NotificationSender> notificationSenders;

    public Order placeOrder(ShoppingCart cart, Governorate governorate) {
        // 1. Rejects an empty cart (ORD-02)
        if (cart == null || cart.isEmpty()) {
            throw new StoreException("Cannot checkout an empty cart.");
        }

        // 2. Re-checks all stock before changing anything (ORD-03)
        Map<String, Integer> cartLines = cart.getLines();
        for (Map.Entry<String, Integer> entry : cartLines.entrySet()) {
            Product product = productService.findBySku(entry.getKey());
            if (product.getStockQuantity() < entry.getValue()) {
                throw new StoreException("Stock changed for '" + product.getName()
                        + "'. Requested: " + entry.getValue()
                        + ", Available: " + product.getStockQuantity());
            }
        }

        // 3. Calculates the totals
        BigDecimal subtotal = cart.subtotal();
        BigDecimal vat = taxCalculator.calculateVat(subtotal);
        BigDecimal shippingFee = shippingQuoteService.quote(governorate, subtotal);
        BigDecimal total = subtotal.add(vat).add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        // 4. Reduces stock (all or none - ORD-06)
        for (Map.Entry<String, Integer> entry : cartLines.entrySet()) {
            productService.reduceStock(entry.getKey(), entry.getValue());
        }

        // 5. Saves the order
        String orderNumber = orderRegistry.nextOrderNumber();
        Order order = new Order(
                orderNumber,
                cart.getCustomer(),
                governorate,
                new LinkedHashMap<>(cartLines),
                subtotal,
                vat,
                shippingFee,
                total
        );
        orderRegistry.save(order);

        // 6. Notifies through every sender in the list (NOT-04)
        String confirmationMsg = String.format(Locale.US,
                "Order %s confirmed. Total %,.2f EGP", orderNumber, total);
        for (NotificationSender sender : notificationSenders) {
            sender.send(cart.getCustomer(), confirmationMsg);
        }

        // 7. Clears the cart
        cart.clear();

        return order;
    }
}
