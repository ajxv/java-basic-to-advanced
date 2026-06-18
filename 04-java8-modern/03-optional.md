# Optional — Eliminating NullPointerException

> Runnable example: [code/modern/OptionalDemo.java](../code/modern/OptionalDemo.java)

---

## The Problem Optional Solves

```java
// Classic null problem: every call chain is a potential NPE
String city = user.getAddress().getCity().toUpperCase(); // any of these can be null

// Optional makes the "might not be there" contract explicit in the type
Optional<String> city = getUser()
    .flatMap(User::getAddress)
    .map(Address::getCity)
    .map(String::toUpperCase);
```

---

## Creating Optional

```java
// Empty Optional
Optional<String> empty = Optional.empty();

// Wrap a non-null value
Optional<String> present = Optional.of("hello");
// Optional.of(null) throws NullPointerException — use ofNullable for unknown values

// Wrap a value that might be null
Optional<String> maybe = Optional.ofNullable(possiblyNullValue);
```

---

## Reading From Optional

```java
Optional<String> opt = Optional.of("hello");

// isPresent / isEmpty — for when you need to branch
if (opt.isPresent()) {
    System.out.println(opt.get()); // .get() on empty Optional throws NoSuchElementException
}

// get() — use only when you've confirmed it's present; prefer alternatives below
String val = opt.get(); // risky if empty

// orElse — return default value if empty (default is always evaluated!)
String result = opt.orElse("default");

// orElseGet — lazy default (evaluated only if empty — prefer for expensive defaults)
String result2 = opt.orElseGet(() -> computeExpensiveDefault());

// orElseThrow — throw exception if empty
String result3 = opt.orElseThrow(() -> new UserNotFoundException("not found"));
// Java 10+: orElseThrow() with no args throws NoSuchElementException

// ifPresent — run action if value exists
opt.ifPresent(s -> System.out.println(s.toUpperCase()));

// ifPresentOrElse — Java 9+: handle both cases
opt.ifPresentOrElse(
    s -> System.out.println("Found: " + s),
    () -> System.out.println("Not found")
);
```

---

## Transforming Optional

This is the real power — chaining without null checks.

```java
// map — transform the value if present, stays empty if empty
Optional<String> upper = Optional.of("hello").map(String::toUpperCase); // Optional["HELLO"]
Optional<String> empty = Optional.<String>empty().map(String::toUpperCase); // Optional.empty

// filter — keep the value only if it passes the predicate
Optional<String> longEnough = Optional.of("hello").filter(s -> s.length() > 3); // Optional["hello"]
Optional<String> tooShort = Optional.of("hi").filter(s -> s.length() > 3);      // Optional.empty

// flatMap — for when the mapper itself returns an Optional
// Without flatMap (nested optionals):
Optional<Optional<Address>> bad = optUser.map(User::getOptionalAddress);

// With flatMap (flattened):
Optional<Address> address = optUser.flatMap(User::getOptionalAddress);
Optional<String> city = address.flatMap(Address::getOptionalCity);

// or — Java 9+: return alternative Optional if empty
Optional<String> result = optUser
    .map(User::getName)
    .or(() -> Optional.of("Anonymous")); // supply alternative if empty
```

---

## Real World Pattern — Service Layer

```java
public class UserService {
    private final UserRepository repo;

    // Return Optional when the value might legitimately not exist
    public Optional<User> findById(String id) {
        return repo.findById(id); // repository returns Optional
    }

    // Chain operations without null checks
    public Optional<String> getUserEmail(String id) {
        return findById(id)
            .filter(User::isActive)
            .map(User::getEmail)
            .filter(email -> email.contains("@"));
    }

    // Throw domain exception when absence is an error
    public User getById(String id) {
        return findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
}

// Caller:
service.getUserEmail("123")
       .ifPresentOrElse(
           email -> sendNotification(email),
           () -> log.warn("No valid email for user 123")
       );
```

---

## When NOT to Use Optional

```java
// 1. Don't use Optional as a method parameter — use overloading or null check
// BAD:
public void process(Optional<String> name) { ... }

// GOOD:
public void process(String name) {
    Objects.requireNonNull(name, "name required");
}
// or two overloads

// 2. Don't use Optional in fields — it's not Serializable and has memory overhead
// BAD:
public class User {
    private Optional<String> nickname; // don't do this
}
// GOOD:
public class User {
    private String nickname; // nullable field is fine
    public Optional<String> getNickname() { return Optional.ofNullable(nickname); }
}

// 3. Don't use Optional for collections — return empty collection instead
// BAD:
public Optional<List<Order>> getOrders(String userId) { ... }
// GOOD:
public List<Order> getOrders(String userId) {
    return repo.findOrders(userId); // return empty list if none
}

// 4. Don't use Optional for primitives — use OptionalInt, OptionalLong, OptionalDouble
OptionalInt maybeInt = OptionalInt.of(42);
int value = maybeInt.orElse(0);
```
