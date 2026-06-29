# Optional — Eliminating NullPointerException

> Runnable example: [code/modern/OptionalDemo.java](../code/modern/OptionalDemo.java)

---

## The Big Picture

> **In plain terms** — `Optional<T>` is a box that either holds a value or is explicitly empty. Its real job isn't runtime magic — it's *honesty in the type signature*. A method returning `Optional<User>` is telling every caller, right in the type, "there might be no user here — you must decide what to do about that." Compare that to returning `User`, which silently might be `null` and blows up later. `Optional` turns "I forgot it could be null" into "the compiler reminds me."

> **Why this matters** — The `NullPointerException` is Java's most common crash (see [exceptions](../03-core-java/01-exception-handling.md#nullpointerexception--diagnosing-the-fix)). `Optional` lets you *chain* operations safely (`map`/`flatMap`/`filter`) so a missing value short-circuits the whole chain instead of throwing — and forces you to handle the empty case at the end. The catch: it's a precise tool for *return types* signalling "might be absent," **not** a universal null replacement. Using it for fields, parameters, or collections (covered at the end) is an anti-pattern.

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

> **In plain terms** — In the first version, any of `getAddress()`, `getCity()` could return null and you'd get an NPE with little clue which one. The `Optional` version reads the same left-to-right, but if *any* step is empty, the whole chain quietly becomes empty — no crash — and you handle "nothing" once, at the end.

> **Going deeper** — The chain works because each step propagates emptiness: a `map`/`flatMap` on an empty `Optional` is a no-op that returns empty. This is the same "railway" pattern functional languages use for error handling. Note the win is *expressiveness and safety*, not raw speed — under the hood it's still null checks plus a wrapper object per step, so don't reach for it in tight numeric loops. Its value is at API boundaries where a caller genuinely must reckon with absence.

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

> **In plain terms** — Three ways in: `Optional.empty()` for "nothing," `Optional.of(x)` when you *know* `x` isn't null, and `Optional.ofNullable(x)` when `x` *might* be null. The distinction matters: `Optional.of(null)` deliberately throws — it's a fail-fast guard against accidentally wrapping a null.

> **Going deeper** — Use `of` as an assertion ("this should never be null; crash loudly if it is") and `ofNullable` as the safe bridge from legacy null-returning APIs into the `Optional` world. A common pattern is wrapping a nullable lookup at the boundary: `return Optional.ofNullable(legacyMap.get(key));`. Don't write `Optional.ofNullable(x).orElse(y)` where a plain `x != null ? x : y` would do — only introduce `Optional` when it crosses an API boundary or feeds a chain.

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

> **In plain terms** — Getting a value out has a spectrum from risky to safe. Avoid `get()` (it throws on empty, defeating the whole point). Prefer `orElse(default)` for a fallback, `orElseThrow(...)` to fail with a meaningful exception, or `ifPresent`/`ifPresentOrElse` to just *act* on the value without unwrapping it. The right one depends on whether "empty" means "use a default," "this is an error," or "do nothing."

> **Going deeper** — The `orElse` vs `orElseGet` distinction is a real performance bug, not pedantry: `orElse(expensive())` *always* evaluates `expensive()` even when the value is present (arguments are eager), while `orElseGet(() -> expensive())` only runs it when actually empty — use `orElseGet` for any non-trivial or side-effecting default. Treat `get()` as a code smell (some linters flag it); if you've already checked `isPresent()`, you've reverted to the manual null-check style `Optional` was meant to replace — chain or unwrap-with-fallback instead.

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

> **In plain terms** — This is `Optional`'s real strength: `map` transforms the value if present (and skips if empty), `filter` drops the value if it fails a test, and `flatMap` is for when your transform *itself* returns an `Optional` (so you don't end up with `Optional<Optional<X>>`). You build a whole conditional pipeline with zero `if (x != null)` checks.

> **Going deeper** — The `map` vs `flatMap` rule is identical to [streams](02-streams.md): use `flatMap` whenever the function returns an `Optional` already, or you'll get a nested optional. This is no coincidence — `Optional` is essentially a stream of zero-or-one elements and shares the same algebra (it even has `.stream()` since Java 9, handy for flattening a `Stream<Optional<T>>` via `flatMap(Optional::stream)`). Chains like `.filter(User::isActive).map(User::getEmail)` read as business rules, which is exactly where `Optional` earns its keep.

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

> **In plain terms** — This is the canonical, correct use: a repository/service `findById` returns `Optional<User>` because the user genuinely might not exist, and callers chain rules onto it (`filter active`, `map to email`) then decide the endgame — provide a default, throw, or branch. The absence is handled explicitly, exactly once, where it matters.

> **Going deeper** — Notice the two distinct caller endings: `getById` converts "absent" into a *domain exception* (when missing is an error the caller can't proceed past), while `getUserEmail` keeps returning `Optional` (when absent is a normal, expected outcome). Choosing between these per method is the core design judgment with `Optional`. This pattern is why JPA/Spring Data repositories return `Optional<T>` from `findById` — it pushes the "what if it's not there?" decision up to the layer that actually knows the answer.

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

> **In plain terms** — `Optional` is a *return-type* tool, full stop. Don't put it in fields (memory overhead, serialization problems — keep a nullable field and expose `Optional` from the getter), don't take it as a parameter (callers then juggle three states: present, empty, *or* a null Optional), and never return `Optional<List>` — an empty list already means "nothing," so just return that.

> **Going deeper** — These rules trace to `Optional`'s design intent (stated by its own authors): it was added specifically as a *method return type* to signal "no result," not as a general Maybe/Option type for every nullable thing. An `Optional` field adds an extra object per instance and isn't `Serializable`; an `Optional` parameter is strictly worse than an overload or `@Nullable`. The "empty collection over `Optional<Collection>`" rule generalizes: prefer a natural empty value (empty list/string/stream, a Null Object) when one exists — reserve `Optional` for when there's genuinely *no* sensible empty value to return.
