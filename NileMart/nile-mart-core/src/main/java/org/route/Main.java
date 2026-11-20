package org.route;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class);
             Scanner scanner = new Scanner(System.in)) {

            ProductService productService = context.getBean(ProductService.class);
            PricingService pricingService = context.getBean(PricingService.class);
            CheckoutService checkoutService = context.getBean(CheckoutService.class);

            Map<Integer, ShoppingCart> carts = new LinkedHashMap<>();
            int nextCartId = 1;

            boolean running = true;
            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();

                try {
                    switch (choice) {
                        case "1" -> showProducts(productService);
                        case "2" -> {
                            System.out.print("Customer full name: ");
                            String name = scanner.nextLine().trim();
                            System.out.print("Customer phone: ");
                            String phone = scanner.nextLine().trim();
                            System.out.print("Customer email (leave blank if none): ");
                            String emailInput = scanner.nextLine().trim();
                            String email = emailInput.isEmpty() ? null : emailInput;

                            ShoppingCart cart = context.getBean(ShoppingCart.class);
                            cart.setCartNumber(nextCartId);
                            cart.setCustomer(new Customer(name, phone, email));
                            carts.put(nextCartId, cart);

                            System.out.println("Created Cart #" + nextCartId + " for " + name);
                            nextCartId++;
                        }
                        case "3" -> {
                            ShoppingCart cart = selectCart(scanner, carts);
                            System.out.print("Enter product SKU: ");
                            String sku = scanner.nextLine().trim().toUpperCase();
                            System.out.print("Enter quantity: ");
                            int qty = Integer.parseInt(scanner.nextLine().trim());

                            cart.addItem(sku, qty);
                            System.out.println("Added " + qty + " x " + sku + " to Cart #" + cart.getCartNumber());
                        }
                        case "4" -> {
                            ShoppingCart cart = selectCart(scanner, carts);
                            showCart(cart, productService, pricingService);
                        }
                        case "5" -> {
                            ShoppingCart cart = selectCart(scanner, carts);
                            System.out.print("Enter Governorate (e.g. GIZA, CAIRO, ASWAN, SOUTH_SINAI): ");
                            String govInput = scanner.nextLine().trim().toUpperCase().replace(" ", "_");
                            Governorate governorate;
                            try {
                                governorate = Governorate.valueOf(govInput);
                            } catch (IllegalArgumentException e) {
                                throw new StoreException("Unknown governorate: " + govInput);
                            }

                            Order order = checkoutService.placeOrder(cart, governorate);
                            printOrderReceipt(order);
                        }
                        case "0" -> running = false;
                        default -> System.out.println("Invalid option. Please choose 0-5.");
                    }
                } catch (StoreException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (NumberFormatException e) {
                    System.out.println("Error: Invalid number format.");
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n========== NILEMART ==========");
        System.out.println("1. Show products");
        System.out.println("2. Create a cart");
        System.out.println("3. Add a product to a cart");
        System.out.println("4. Show a cart");
        System.out.println("5. Check out a cart");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private static ShoppingCart selectCart(Scanner scanner, Map<Integer, ShoppingCart> carts) {
        System.out.print("Enter Cart #: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        ShoppingCart cart = carts.get(id);
        if (cart == null) {
            throw new StoreException("Cart #" + id + " does not exist.");
        }
        return cart;
    }

    private static void showProducts(ProductService productService) {
        System.out.println("\n--- Product Catalog ---");
        System.out.printf("%-13s | %-26s | %-15s | %10s | %5s%n",
                "SKU", "Name", "Category", "Price", "Stock");
        System.out.println("-".repeat(80));
        for (Product p : productService.findAll()) {
            System.out.printf(Locale.US, "%-13s | %-26s | %-15s | %,10.2f | %5d%n",
                    p.getSku(), p.getName(), p.getCategory(), p.getPrice(), p.getStockQuantity());
        }
    }

    private static void showCart(ShoppingCart cart, ProductService productService, PricingService pricingService) {
        System.out.println("\n--- Cart #" + cart.getCartNumber() + " (" + cart.getCustomer().fullName() + ") ---");
        if (cart.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        for (Map.Entry<String, Integer> entry : cart.getLines().entrySet()) {
            Product p = productService.findBySku(entry.getKey());
            System.out.printf(Locale.US, "%d x %-25s (%s) = %,.2f EGP%n",
                    entry.getValue(), p.getName(), p.getSku(), pricingService.getPrice(p, entry.getValue()));
        }
        System.out.printf(Locale.US, "Subtotal: %,.2f EGP%n", cart.subtotal());
    }

    private static void printOrderReceipt(Order order) {
        System.out.println("\n--- Order Receipt ---");
        System.out.println("Order Number : " + order.orderNumber());
        System.out.println("Customer     : " + order.customer().fullName());
        System.out.println("Governorate  : " + order.governorate() + " (" + order.governorate().getZone() + ")");
        System.out.printf(Locale.US, "Subtotal     : %,.2f EGP%n", order.subtotal());
        System.out.printf(Locale.US, "VAT (14%%)    : %,.2f EGP%n", order.vat());
        System.out.printf(Locale.US, "Shipping     : %,.2f EGP%n", order.shippingFee());
        System.out.printf(Locale.US, "Total        : %,.2f EGP%n", order.total());
    }
}
