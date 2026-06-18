# Design Patterns You'll Actually Use

> Runnable example: [code/advanced/PatternsDemo.java](../code/advanced/PatternsDemo.java)

Patterns are reusable solutions to common design problems. Learn to recognize the problem first — then the pattern is obvious.

---

## Singleton — One Instance Only

**Problem:** You need exactly one instance of a class (config, connection pool, registry).

```java
// Thread-safe lazy singleton using holder idiom:
public class AppConfig {
    private final String dbUrl;
    private final int maxConnections;

    private AppConfig() {
        this.dbUrl = System.getenv("DB_URL");
        this.maxConnections = Integer.parseInt(System.getenv().getOrDefault("MAX_CONN", "10"));
    }

    private static class Holder {
        // Initialized on first access to Holder — lazy, thread-safe
        static final AppConfig INSTANCE = new AppConfig();
    }

    public static AppConfig getInstance() {
        return Holder.INSTANCE;
    }

    public String getDbUrl() { return dbUrl; }
    public int getMaxConnections() { return maxConnections; }
}

// Simplest thread-safe singleton: enum
public enum DatabasePool {
    INSTANCE;
    private final DataSource dataSource = createDataSource();
    public DataSource get() { return dataSource; }
}
```

---

## Builder — Complex Object Construction

**Problem:** A class has many parameters, some optional. Constructors with 8+ parameters are unreadable.

```java
public class EmailMessage {
    private final String to;
    private final String subject;
    private final String body;
    private final List<String> cc;
    private final List<String> attachments;
    private final boolean isHtml;

    private EmailMessage(Builder builder) {
        this.to = builder.to;
        this.subject = builder.subject;
        this.body = builder.body;
        this.cc = List.copyOf(builder.cc);
        this.attachments = List.copyOf(builder.attachments);
        this.isHtml = builder.isHtml;
    }

    public static class Builder {
        // Required
        private final String to;
        private final String subject;
        // Optional with defaults
        private String body = "";
        private List<String> cc = new ArrayList<>();
        private List<String> attachments = new ArrayList<>();
        private boolean isHtml = false;

        public Builder(String to, String subject) {
            this.to = Objects.requireNonNull(to, "to");
            this.subject = Objects.requireNonNull(subject, "subject");
        }
        public Builder body(String body) { this.body = body; return this; }
        public Builder cc(String... emails) { this.cc = List.of(emails); return this; }
        public Builder attach(String path) { this.attachments.add(path); return this; }
        public Builder html() { this.isHtml = true; return this; }

        public EmailMessage build() { return new EmailMessage(this); }
    }
}

// Usage — reads like English:
EmailMessage email = new EmailMessage.Builder("alice@example.com", "Hello!")
    .body("<h1>Welcome</h1>")
    .cc("manager@example.com")
    .attach("/tmp/report.pdf")
    .html()
    .build();
```

---

## Factory — Decouple Creation from Use

**Problem:** You need to create objects but don't want callers to know the exact type.

```java
public interface NotificationSender {
    void send(String recipient, String message);
}

public class EmailSender implements NotificationSender { ... }
public class SmsSender implements NotificationSender { ... }
public class PushSender implements NotificationSender { ... }

// Factory — central creation logic
public class NotificationSenderFactory {
    public static NotificationSender create(String channel) {
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> new EmailSender();
            case "SMS"   -> new SmsSender();
            case "PUSH"  -> new PushSender();
            default -> throw new IllegalArgumentException("Unknown channel: " + channel);
        };
    }
}

// Caller doesn't know which implementation it gets:
NotificationSender sender = NotificationSenderFactory.create(config.getChannel());
sender.send(user.getEmail(), "Your order shipped!");
// Easy to add new channels without changing callers
```

---

## Strategy — Swappable Algorithms

**Problem:** You need to switch between different algorithms or behaviors at runtime.

```java
@FunctionalInterface
public interface PricingStrategy {
    double calculate(double basePrice, int quantity);
}

public class Order {
    private PricingStrategy pricingStrategy;

    public Order(PricingStrategy strategy) {
        this.pricingStrategy = strategy;
    }

    public void setPricingStrategy(PricingStrategy strategy) {
        this.pricingStrategy = strategy;
    }

    public double total(double basePrice, int quantity) {
        return pricingStrategy.calculate(basePrice, quantity);
    }
}

// Strategies as lambdas:
PricingStrategy regular  = (price, qty) -> price * qty;
PricingStrategy bulk     = (price, qty) -> qty >= 10 ? price * qty * 0.9 : price * qty;
PricingStrategy employee = (price, qty) -> price * qty * 0.7;

Order order = new Order(regular);
order.total(10.0, 5);  // 50.0

order.setPricingStrategy(bulk);
order.total(10.0, 15); // 135.0 (10% discount)
```

---

## Observer — Event Notification

**Problem:** When something happens, multiple independent objects need to be notified.

```java
// Generic event bus
public class EventBus<T> {
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<T> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<T> listener) {
        listeners.remove(listener);
    }

    public void publish(T event) {
        listeners.forEach(l -> l.accept(event));
    }
}

// Domain event:
public record OrderPlacedEvent(String orderId, String userId, double amount) {}

// Wiring:
EventBus<OrderPlacedEvent> bus = new EventBus<>();
bus.subscribe(event -> emailService.sendConfirmation(event.userId()));
bus.subscribe(event -> inventoryService.reserve(event.orderId()));
bus.subscribe(event -> analyticsService.track(event));

// When an order is placed:
bus.publish(new OrderPlacedEvent("ORD-001", "USR-123", 99.99));
// All three listeners are notified automatically
```

---

## Decorator — Add Behavior Dynamically

**Problem:** You want to add features to an object without changing its class or using inheritance.

```java
public interface DataSource {
    void write(String data);
    String read();
}

// Base implementation:
public class FileDataSource implements DataSource {
    private final Path path;
    public FileDataSource(String filename) { this.path = Path.of(filename); }
    public void write(String data) { Files.writeString(path, data); }
    public String read() { return Files.readString(path); }
}

// Decorator: adds encryption
public class EncryptionDecorator implements DataSource {
    private final DataSource wrapped;
    public EncryptionDecorator(DataSource source) { this.wrapped = source; }

    public void write(String data) {
        wrapped.write(encrypt(data)); // encrypt then delegate
    }
    public String read() {
        return decrypt(wrapped.read()); // delegate then decrypt
    }
}

// Decorator: adds compression
public class CompressionDecorator implements DataSource {
    private final DataSource wrapped;
    public CompressionDecorator(DataSource source) { this.wrapped = source; }

    public void write(String data) { wrapped.write(compress(data)); }
    public String read() { return decompress(wrapped.read()); }
}

// Stacking decorators:
DataSource source = new EncryptionDecorator(
                        new CompressionDecorator(
                            new FileDataSource("data.dat")));

source.write("secret data"); // compress → encrypt → write to file
source.read();               // read → decrypt → decompress
```

---

## Repository — Separate Data Access

**Problem:** Business logic should not know about databases or SQL.

```java
// Interface: what operations are available
public interface UserRepository {
    Optional<User> findById(String id);
    List<User> findByDepartment(String dept);
    void save(User user);
    void delete(String id);
}

// Implementation: actual DB access (hidden from business logic)
public class JdbcUserRepository implements UserRepository {
    private final DataSource dataSource;

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        }
    }
    // ... other methods
}

// Business logic only knows the interface:
public class UserService {
    private final UserRepository repo; // injected — could be JDBC or in-memory for tests

    public User getActiveUser(String id) {
        return repo.findById(id)
                   .filter(User::isActive)
                   .orElseThrow(() -> new UserNotFoundException(id));
    }
}

// In tests: use a simple in-memory implementation — no DB needed
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> store = new HashMap<>();
    public Optional<User> findById(String id) { return Optional.ofNullable(store.get(id)); }
    public void save(User u) { store.put(u.getId(), u); }
    // ...
}
```
