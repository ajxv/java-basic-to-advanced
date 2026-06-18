import java.util.*;

public class OptionalDemo {

    record User(String id, String name, String email, boolean active) {}
    record Address(String street, String city, String country) {}

    static Map<String, User> userDb = Map.of(
        "USR-001", new User("USR-001", "Alice", "alice@example.com", true),
        "USR-002", new User("USR-002", "Bob", null, true),
        "USR-003", new User("USR-003", "Charlie", "charlie@example.com", false)
    );

    public static void main(String[] args) {
        creatingOptional();
        readingOptional();
        transformingOptional();
        chainingOptional();
        optionalDontDo();
    }

    static void creatingOptional() {
        System.out.println("=== Creating Optional ===");

        Optional<String> empty = Optional.empty();
        Optional<String> present = Optional.of("hello");        // throws NPE if null
        Optional<String> maybe = Optional.ofNullable(null);     // safe for null

        System.out.println("empty: " + empty);
        System.out.println("present: " + present);
        System.out.println("ofNullable(null): " + maybe);
        System.out.println("ofNullable('hi'): " + Optional.ofNullable("hi"));
    }

    static void readingOptional() {
        System.out.println("\n=== Reading Optional ===");

        Optional<String> opt = Optional.of("hello");
        Optional<String> empty = Optional.empty();

        // isPresent / isEmpty
        System.out.println("isPresent: " + opt.isPresent());  // true
        System.out.println("isEmpty: " + empty.isEmpty());    // true

        // get() — only safe after isPresent check (but avoid this style)
        if (opt.isPresent()) {
            System.out.println("get(): " + opt.get());
        }

        // orElse — always evaluated (even for expensive operations, use orElseGet)
        System.out.println("orElse: " + empty.orElse("default"));

        // orElseGet — lazily evaluated
        System.out.println("orElseGet: " + empty.orElseGet(() -> "computed default"));

        // orElseThrow
        try {
            empty.orElseThrow(() -> new NoSuchElementException("Not found"));
        } catch (NoSuchElementException e) {
            System.out.println("orElseThrow: " + e.getMessage());
        }

        // ifPresent — run action only if present
        opt.ifPresent(s -> System.out.println("ifPresent: " + s.toUpperCase()));
        empty.ifPresent(s -> System.out.println("never printed"));

        // ifPresentOrElse (Java 9+)
        opt.ifPresentOrElse(
            s -> System.out.println("ifPresentOrElse: found=" + s),
            () -> System.out.println("ifPresentOrElse: not found")
        );
        empty.ifPresentOrElse(
            s -> System.out.println("never"),
            () -> System.out.println("ifPresentOrElse: not found")
        );
    }

    static void transformingOptional() {
        System.out.println("\n=== Transforming Optional ===");

        Optional<String> opt = Optional.of("  hello world  ");

        // map — transform the value if present
        Optional<String> upper = opt.map(String::trim).map(String::toUpperCase);
        System.out.println("map: " + upper); // Optional[HELLO WORLD]

        Optional<Integer> length = opt.map(String::length);
        System.out.println("map to length: " + length); // Optional[15]

        // Empty stays empty through map
        Optional<String> empty = Optional.<String>empty().map(String::toUpperCase);
        System.out.println("empty.map: " + empty); // Optional.empty

        // filter — keep value only if predicate holds
        Optional<String> longEnough = opt.filter(s -> s.length() > 5);
        System.out.println("filter long>5: " + longEnough.isPresent()); // true

        Optional<String> tooShort = Optional.of("hi").filter(s -> s.length() > 5);
        System.out.println("filter short>5: " + tooShort.isPresent()); // false

        // or (Java 9+) — alternative Optional if empty
        Optional<String> fallback = empty.or(() -> Optional.of("fallback"));
        System.out.println("or fallback: " + fallback); // Optional[fallback]
    }

    static Optional<User> findUser(String id) {
        return Optional.ofNullable(userDb.get(id));
    }

    static Optional<String> getUserEmail(String id) {
        return findUser(id)
            .filter(User::active)           // only active users
            .map(User::email)               // get email (might be null)
            .filter(Objects::nonNull)       // filter out null emails
            .filter(e -> e.contains("@")); // basic email validation
    }

    static void chainingOptional() {
        System.out.println("\n=== Chaining Optional — No Null Checks ===");

        // Without Optional:
        // User user = userDb.get(id);
        // if (user != null && user.active() && user.email() != null && user.email().contains("@")) ...

        // With Optional — reads like a pipeline
        for (String id : List.of("USR-001", "USR-002", "USR-003", "USR-999")) {
            String result = getUserEmail(id).orElse("no-valid-email");
            System.out.printf("  %s → %s%n", id, result);
        }
        // USR-001 → alice@example.com    (active, has email)
        // USR-002 → no-valid-email       (active, but null email)
        // USR-003 → no-valid-email       (inactive)
        // USR-999 → no-valid-email       (not found)

        // flatMap: when your mapping function also returns Optional
        // (avoids Optional<Optional<T>>)
        Optional<String> city = findUser("USR-001")
            .flatMap(u -> getAddress(u.id()))   // returns Optional<Address>
            .map(Address::city);                // map Address → String
        System.out.println("City: " + city.orElse("unknown"));
    }

    static Optional<Address> getAddress(String userId) {
        // Simulated — some users have no address
        if ("USR-001".equals(userId)) {
            return Optional.of(new Address("123 Main St", "Springfield", "US"));
        }
        return Optional.empty();
    }

    static void optionalDontDo() {
        System.out.println("\n=== Optional — What NOT To Do ===");

        // 1. Don't use as method parameter
        // BAD: public void process(Optional<String> name)
        // GOOD: overload or accept String and check inside

        // 2. Don't use in fields (not Serializable, memory overhead)
        // BAD: private Optional<String> nickname;
        // GOOD: private String nickname; (nullable)
        //       public Optional<String> getNickname() { return Optional.ofNullable(nickname); }

        // 3. Don't use for collections — return empty collection instead
        // BAD: Optional<List<String>> getItems()
        // GOOD: List<String> getItems() — return empty list if none

        // 4. Don't call get() without isPresent() check
        Optional<String> opt = Optional.empty();
        try {
            opt.get(); // throws NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("get() on empty throws: " + e.getClass().getSimpleName());
        }
        // Use orElse, orElseGet, orElseThrow instead of get()

        // 5. Use OptionalInt/OptionalLong/OptionalDouble for primitives (avoids boxing)
        OptionalInt maybeInt = OptionalInt.of(42);
        System.out.println("OptionalInt: " + maybeInt.orElse(0));

        System.out.println("\nCorrect pattern summary:");
        System.out.println("  - findX() returns Optional<X>");
        System.out.println("  - getX() throws if not found (calls orElseThrow)");
        System.out.println("  - Use map/flatMap/filter to chain, not isPresent+get");
    }
}
