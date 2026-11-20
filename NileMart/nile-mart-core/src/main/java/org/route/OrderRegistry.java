package org.route;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OrderRegistry {

    private final List<Order> orders = new ArrayList<>();
    private int sequence = 0;

    public String nextOrderNumber() {
        sequence++;
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("NM-%s-%05d", datePart, sequence);
    }

    public void save(Order order) {
        orders.add(order);
    }

    @PreDestroy
    public void printEndOfDaySummary() {
        BigDecimal totalRevenue = orders.stream()
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        System.out.printf(Locale.US, "End of day: %d orders, revenue %,.2f EGP%n",
                orders.size(), totalRevenue);
    }
}
