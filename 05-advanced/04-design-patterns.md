# Design Patterns You'll Actually Use

> Runnable example: [code/advanced/PatternsDemo.java](../code/advanced/PatternsDemo.java)

Patterns are reusable solutions to common design problems. Learn to recognize the problem first — then the pattern is obvious.

---

## The Big Picture

> **In plain terms** — Design patterns are *named, proven solutions* to problems that show up again and again in software. They're not libraries you import — they're shapes you recognize. The value is twofold: they save you from reinventing a solution, and they give teams a shared vocabulary ("let's make that a Strategy") that conveys a whole design in one word. The patterns in this doc are the handful you'll genuinely meet in everyday Java.

> **Why this matters** — Used well, patterns make code flexible exactly where it needs to flex (swap an algorithm, add a behavior, hide a data source). Used badly — forced in where a simpler solution fits — they add ceremony and indirection for no gain. So the skill isn't memorizing 23 patterns; it's recognizing the *problem* first ("I have many optional constructor params," "I need to swap behavior at runtime") and reaching for the pattern only when it earns its keep. Notice, too, that lambdas and interfaces ([part 4](../04-java8-modern/01-lambdas-and-functional.md), [interfaces](../02-oop/03-interfaces-and-abstract-classes.md)) have made several classic patterns nearly disappear into one-liners.

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

> **In plain terms** — A Singleton guarantees there's exactly *one* instance of something (a config holder, a connection pool) and gives everyone a shared way to reach it. The two safe ways in Java are the *holder idiom* (lazy, thread-safe via class-loading rules) and — simplest of all — an `enum` with one constant, which the JVM guarantees is a single instance.

> **Going deeper** — Why `enum` is the gold standard (per *Effective Java*): the JVM handles thread-safe lazy init *and* protects against the two ways other singletons break — reflection (can't `new` an enum) and deserialization (enums deserialize to the same instance). The holder idiom works because a class isn't initialized until first referenced, so `Holder.INSTANCE` is created lazily and thread-safely with zero locking. Caveat: Singleton is the most *overused* pattern — it's global mutable state in disguise, which hurts testability and hides dependencies. In modern apps, prefer a *dependency-injection* container (Spring) to manage single instances and inject them, rather than a hard-coded `getInstance()` callers can't substitute in tests.

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

> **In plain terms** — When a class has lots of parameters — especially optional ones — a constructor like `new EmailMessage(to, subj, body, cc, att, true)` is unreadable and easy to get wrong (which `true` was that?). A Builder lets you set only what you need by name, in any order, ending with `.build()`. The result reads like a sentence and makes the final object immutable.

> **Going deeper** — The Builder solves the *telescoping constructor* problem (a ladder of overloads for every combo) and the *JavaBeans* problem (setters leave the object mutable and temporarily invalid). Required fields go in the builder's constructor (fail fast if missing); optional ones get fluent setters returning `this`. Validation lives in `build()`. You rarely hand-write builders anymore: Lombok's `@Builder` generates one, and for plain immutable data a [`record`](../04-java8-modern/04-modern-java-9-to-21.md#java-14--records) replaces the need entirely (use a builder only when you have many *optional* params). This also pairs with the "keep parameter lists short" advice from [methods](../01-basics/04-methods.md#method-design-tips).

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

> **In plain terms** — A Factory is one central place that decides *which* concrete class to create, so the rest of your code asks for "a notification sender" without knowing or caring whether it's email, SMS, or push. Add a new channel and you change the factory only — every caller keeps working unchanged.

> **Going deeper** — This is *programming to an interface*: callers depend on `NotificationSender`, not its implementations (the dependency-inversion seam from [interfaces](../02-oop/03-interfaces-and-abstract-classes.md)). Factories also give you a natural spot for centralized concerns — caching instances, reading config, wiring dependencies. "Factory" spans a few variants: a simple static factory *method* (`Optional.of`, `List.of`), this parameterized factory, and the full *Abstract Factory* (a family of related factories). Don't over-engineer — a static method is often plenty, and DI frameworks subsume much of this in real apps.

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

> **In plain terms** — Strategy lets you swap *how* something is done at runtime. An `Order` doesn't hard-code its pricing math; it holds a `PricingStrategy` you can change (regular, bulk, employee). Each strategy is interchangeable because they share one interface. Notice each is just a lambda — in modern Java, Strategy is often "pass a function."

> **Going deeper** — Strategy is the antidote to sprawling `if/else`/`switch` chains that branch on a "type" or "mode" — instead of editing that switch every time, you add a new strategy object. Because a [functional interface](../02-oop/03-interfaces-and-abstract-classes.md#functional-interfaces) + lambda *is* a strategy, much of the classic boilerplate vanished in Java 8 — `Comparator` passed to `sort` is Strategy, every `Function`/`Predicate` you pass to a [stream](../04-java8-modern/02-streams.md) is Strategy. Keep strategies stateless and you can share/cache single instances. The line between Strategy and a plain callback is mostly intent: Strategy names a *family* of interchangeable algorithms for one well-defined job.

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

> **In plain terms** — Observer lets one thing announce "this happened!" and any number of interested parties react — without the announcer knowing who's listening. When an order is placed, the email, inventory, and analytics services all respond, but the order code just calls `publish`. You can add or remove listeners freely; the publisher is untouched.

> **Going deeper** — This decouples *what happened* from *what to do about it* — the foundation of event-driven architecture, UI frameworks, and pub/sub messaging. Real-world cautions: listener references are a classic [memory leak](03-memory-and-gc.md#2-event-listeners-not-removed) (always provide `unsubscribe`), notification order isn't guaranteed, and one slow or throwing listener can block the rest (note the `CopyOnWriteArrayList` here makes iteration thread-safe during concurrent subscribe/unsubscribe). For anything serious, lean on existing infrastructure — Spring's `ApplicationEvent`, a message broker (Kafka/RabbitMQ), or `java.util.concurrent.Flow` (reactive streams) — rather than a hand-rolled bus.

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

> **In plain terms** — A Decorator wraps an object in another object that shares the same interface, adding behavior before/after delegating to the wrapped one. Because each layer has the same type, you can stack them like Russian dolls — wrap a file source in compression, then in encryption — and combine features without writing a class for every combination.

> **Going deeper** — Decorator is *composition over inheritance* in action: instead of a class explosion (`EncryptedCompressedFileSource`...), you compose small wrappers at runtime in any order. You've used it without naming it — `new BufferedReader(new InputStreamReader(inputStream))` is the JDK's I/O layering, the textbook example. Trade-offs: many thin layers can make stack traces and debugging noisier, and the order of wrapping matters (encrypt-then-compress vs compress-then-encrypt behave differently). Keep each decorator focused on one concern and faithful to the wrapped contract.

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

> **In plain terms** — A Repository hides *where and how* data is stored behind a plain interface of operations (`findById`, `save`, `delete`). Your business logic talks to `UserRepository` and never sees SQL or connections. The payoff is huge for testing: swap the real JDBC implementation for a simple in-memory `Map` and your service tests run instantly with no database.

> **Going deeper** — Repository keeps persistence concerns out of your domain logic (separation of concerns) and makes the data layer swappable (JDBC today, a different store tomorrow) — it's essentially the [interface-driven testability](../02-oop/03-interfaces-and-abstract-classes.md) seam applied to data access. It builds on [exceptions](../03-core-java/01-exception-handling.md) (translate `SQLException` into domain exceptions so callers don't depend on JDBC) and [`Optional`](../04-java8-modern/03-optional.md) (return `Optional<User>` for "might not exist"). In practice, Spring Data generates the implementation from the interface alone — you declare `findByDepartment` and it writes the query — which is this pattern taken to its logical conclusion. Keep repositories focused on persistence; business rules belong in the service layer above.
