import java.util.*;
import java.util.function.*;

public class PatternsDemo {

    public static void main(String[] args) {
        builderPattern();
        factoryPattern();
        strategyPattern();
        observerPattern();
        decoratorPattern();
    }

    // === Builder Pattern ===

    static class HttpRequest {
        private final String url;
        private final String method;
        private final Map<String, String> headers;
        private final String body;
        private final int timeoutMs;

        private HttpRequest(Builder b) {
            this.url = b.url;
            this.method = b.method;
            this.headers = Collections.unmodifiableMap(b.headers);
            this.body = b.body;
            this.timeoutMs = b.timeoutMs;
        }

        @Override
        public String toString() {
            return String.format("HttpRequest{%s %s, headers=%s, timeout=%dms}",
                method, url, headers, timeoutMs);
        }

        static class Builder {
            private final String url;
            private String method = "GET";
            private Map<String, String> headers = new LinkedHashMap<>();
            private String body;
            private int timeoutMs = 5000;

            Builder(String url) {
                this.url = Objects.requireNonNull(url, "URL required");
            }

            Builder method(String method) { this.method = method; return this; }
            Builder header(String key, String val) { headers.put(key, val); return this; }
            Builder body(String body) { this.body = body; return this; }
            Builder timeout(int ms) { this.timeoutMs = ms; return this; }
            HttpRequest build() { return new HttpRequest(this); }
        }
    }

    static void builderPattern() {
        System.out.println("=== Builder Pattern ===");
        HttpRequest req = new HttpRequest.Builder("https://api.example.com/users")
            .method("POST")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer token123")
            .body("{\"name\":\"Alice\"}")
            .timeout(3000)
            .build();
        System.out.println(req);
    }

    // === Factory Pattern ===

    interface NotificationSender {
        void send(String recipient, String message);
    }

    static class EmailSender implements NotificationSender {
        @Override
        public void send(String to, String msg) {
            System.out.println("[EMAIL] To: " + to + " | " + msg);
        }
    }

    static class SmsSender implements NotificationSender {
        @Override
        public void send(String to, String msg) {
            System.out.println("[SMS] To: " + to + " | " + msg);
        }
    }

    static class PushSender implements NotificationSender {
        @Override
        public void send(String to, String msg) {
            System.out.println("[PUSH] To: " + to + " | " + msg);
        }
    }

    static NotificationSender createSender(String channel) {
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> new EmailSender();
            case "SMS"   -> new SmsSender();
            case "PUSH"  -> new PushSender();
            default -> throw new IllegalArgumentException("Unknown channel: " + channel);
        };
    }

    static void factoryPattern() {
        System.out.println("\n=== Factory Pattern ===");
        for (String channel : List.of("EMAIL", "SMS", "PUSH")) {
            NotificationSender sender = createSender(channel);
            sender.send("user@example.com", "Your order has shipped!");
        }
    }

    // === Strategy Pattern ===

    @FunctionalInterface
    interface PricingStrategy {
        double calculate(double basePrice, int quantity);
    }

    static class ShoppingCart {
        private PricingStrategy strategy;
        ShoppingCart(PricingStrategy strategy) { this.strategy = strategy; }
        void setStrategy(PricingStrategy strategy) { this.strategy = strategy; }
        double total(double price, int qty) { return strategy.calculate(price, qty); }
    }

    static void strategyPattern() {
        System.out.println("\n=== Strategy Pattern ===");

        PricingStrategy regular  = (price, qty) -> price * qty;
        PricingStrategy bulk     = (price, qty) -> qty >= 10 ? price * qty * 0.9 : price * qty;
        PricingStrategy vip      = (price, qty) -> price * qty * 0.8;

        ShoppingCart cart = new ShoppingCart(regular);
        System.out.printf("Regular 5x$10: $%.2f%n", cart.total(10, 5));  // $50.00

        cart.setStrategy(bulk);
        System.out.printf("Bulk 15x$10:   $%.2f%n", cart.total(10, 15)); // $135.00 (10% off)

        cart.setStrategy(vip);
        System.out.printf("VIP 5x$10:     $%.2f%n", cart.total(10, 5));  // $40.00 (20% off)
    }

    // === Observer / EventBus Pattern ===

    static class EventBus<T> {
        private final List<Consumer<T>> listeners = new ArrayList<>();

        void subscribe(Consumer<T> listener) { listeners.add(listener); }
        void unsubscribe(Consumer<T> listener) { listeners.remove(listener); }
        void publish(T event) { listeners.forEach(l -> l.accept(event)); }
    }

    record OrderEvent(String orderId, String customerId, double amount) {}

    static void observerPattern() {
        System.out.println("\n=== Observer / EventBus ===");

        EventBus<OrderEvent> bus = new EventBus<>();

        // Subscribe multiple independent listeners
        bus.subscribe(e -> System.out.println("[EMAIL] Confirmation for order " + e.orderId()));
        bus.subscribe(e -> System.out.println("[INVENTORY] Reserve items for " + e.orderId()));
        bus.subscribe(e -> System.out.printf("[ANALYTICS] Order %.2f from %s%n", e.amount(), e.customerId()));

        // One event, three handlers
        bus.publish(new OrderEvent("ORD-001", "CUST-123", 99.99));
    }

    // === Decorator Pattern ===

    interface TextProcessor {
        String process(String text);
    }

    static class TrimDecorator implements TextProcessor {
        private final TextProcessor wrapped;
        TrimDecorator(TextProcessor wrapped) { this.wrapped = wrapped; }
        @Override
        public String process(String text) { return wrapped.process(text.trim()); }
    }

    static class UpperCaseDecorator implements TextProcessor {
        private final TextProcessor wrapped;
        UpperCaseDecorator(TextProcessor wrapped) { this.wrapped = wrapped; }
        @Override
        public String process(String text) { return wrapped.process(text).toUpperCase(); }
    }

    static class PunctuationDecorator implements TextProcessor {
        private final TextProcessor wrapped;
        PunctuationDecorator(TextProcessor wrapped) { this.wrapped = wrapped; }
        @Override
        public String process(String text) { return wrapped.process(text) + "!"; }
    }

    static void decoratorPattern() {
        System.out.println("\n=== Decorator Pattern ===");

        // Base processor: identity
        TextProcessor base = text -> text;

        // Stack decorators — each wraps the previous
        TextProcessor trimmed = new TrimDecorator(base);
        TextProcessor upper   = new UpperCaseDecorator(trimmed);
        TextProcessor shout   = new PunctuationDecorator(upper);

        String input = "  hello world  ";
        System.out.println("Input:   '" + input + "'");
        System.out.println("Trimmed: '" + trimmed.process(input) + "'");
        System.out.println("Upper:   '" + upper.process(input) + "'");
        System.out.println("Shout:   '" + shout.process(input) + "'");
    }
}
