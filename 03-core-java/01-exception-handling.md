# Exception Handling

> Runnable example: [code/core/ExceptionDemo.java](../code/core/ExceptionDemo.java)

---

## The Big Picture

> **In plain terms** — An exception is Java's way of saying "something went wrong here, and I can't sensibly continue." Instead of returning a magic error code that callers might ignore, the failure *interrupts* the normal flow and jumps to the nearest piece of code that says it knows how to handle that kind of problem (`catch`). Exception handling is about three questions: *where* do I detect the problem, *who* is in a position to do something about it, and *what* cleanup must happen no matter what.

> **Why this matters** — The golden rule is **handle exceptions where you can actually do something useful** — not wherever they happen to pop up. A low-level method usually can't decide how to recover, so it lets the exception bubble up (often wrapped in a meaningful type) to a layer that can retry, show the user a message, or abort cleanly. Done well, error handling is invisible; done badly (swallowed exceptions, lost stack traces), it produces the worst bugs — failures with no trail.

---

## The Exception Hierarchy

```
Throwable
├── Error             ← JVM failures (OutOfMemoryError, StackOverflowError)
│                       DO NOT catch these — let the app crash
└── Exception
    ├── RuntimeException  ← Unchecked — programming mistakes
    │   ├── NullPointerException
    │   ├── IllegalArgumentException
    │   ├── IllegalStateException
    │   ├── IndexOutOfBoundsException
    │   └── ClassCastException
    └── (other Exception subclasses) ← Checked — must handle or declare
        ├── IOException
        ├── SQLException
        └── FileNotFoundException
```

> **In plain terms** — Everything throwable descends from `Throwable`, which splits into two worlds. `Error` is the JVM saying "I'm in serious trouble" (out of memory, stack blown) — you don't catch these. `Exception` is application-level problems you might handle, and it further splits into *checked* (the compiler makes you deal with them) and *unchecked* (`RuntimeException` and friends — usually your bugs).

> **Going deeper** — `RuntimeException` and `Error` are the "unchecked" branches; everything else under `Exception` is "checked." The practical takeaway: catching `Throwable` or `Exception` too broadly will also swallow things like `InterruptedException` (which needs special handling) and can hide `Error`s you should let crash. Catch the narrowest type that you can actually act on. Note `NullPointerException`, `ArrayIndexOutOfBoundsException`, etc. are all unchecked because they signal a code defect — the fix is correct code, not a `catch`.

---

## Checked vs Unchecked

```java
// Checked exception: compiler forces you to handle it or declare throws
public String readFile(String path) throws IOException {
    return Files.readString(Path.of(path)); // compiler: "handle IOException or throws it"
}

// Caller must handle it:
try {
    String content = readFile("data.txt");
} catch (IOException e) {
    System.err.println("Failed: " + e.getMessage());
}

// Unchecked exception: no compiler enforcement — your responsibility to prevent
public String getFirst(List<String> list) {
    return list.get(0); // IndexOutOfBoundsException if empty — no warning from compiler
}
```

**Guideline:** Use unchecked exceptions for programming errors (wrong input, illegal state). Use checked exceptions for recoverable I/O or external failures where the caller *should* do something.

> **In plain terms** — *Checked* exceptions are ones the compiler refuses to let you ignore — you must either `catch` them or declare `throws`. They're meant for failures the caller can reasonably expect and recover from (a file isn't there, the network blipped). *Unchecked* exceptions need no ceremony and are meant for bugs (you passed null, the index was out of range) — things you fix in code, not handle at runtime.

> **Going deeper** — Checked exceptions are a famously divisive Java feature. They make failure modes explicit and impossible to silently drop — but overused, they pollute signatures and tempt developers into the `catch (e) {}` anti-pattern. Modern practice (and most major frameworks like Spring) leans toward *unchecked* exceptions, especially across API boundaries and inside lambdas/streams (which can't propagate checked exceptions cleanly). A common pattern: wrap a low-level checked exception (`IOException`) in a meaningful unchecked domain exception (`ConfigLoadException`) so callers aren't forced to handle plumbing details they can't fix.

---

## try-catch-finally

```java
Connection conn = null;
try {
    conn = dataSource.getConnection();
    // DB work
    String result = executeQuery(conn);
    return result;

} catch (SQLException e) {
    // Handle the error
    logger.error("DB query failed", e); // always log full exception, not just message
    throw new ServiceException("Database error", e); // wrap in domain exception

} finally {
    // Always runs — even if exception thrown or return executed
    if (conn != null) {
        try { conn.close(); } catch (SQLException e) { logger.warn("Close failed", e); }
    }
}
```

> **In plain terms** — `try` runs the risky code, `catch` handles a failure, and `finally` runs *no matter what* — exception or not, even after a `return`. `finally` is where you put cleanup (closing connections, files) that must happen either way. The verbose null-check-and-close dance here is exactly the pain that try-with-resources (next section) was invented to remove.

> **Going deeper** — Two classic `finally` traps: (1) a `return` or `throw` *inside* `finally` will silently override whatever the `try`/`catch` was returning or throwing — including swallowing the original exception, so never return from `finally`; (2) `finally` runs even when `try` returns, so it can quietly change the result. Note that the bare `System.exit()` and a killed JVM are the only things that skip `finally`. Wrapping the original `SQLException` with `new ServiceException("...", e)` preserves the *cause chain* (see re-throw below) so you don't lose the root error.

---

## try-with-resources (Preferred)

Any class implementing `AutoCloseable` can be used. The resource is automatically closed, even if an exception occurs.

```java
// Resources listed in try(...) are auto-closed in reverse order
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {

    ps.setString(1, userId);
    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            process(rs);
        }
    }
} catch (SQLException e) {
    // conn, ps, and rs are already closed here
    throw new ServiceException("Query failed", e);
}
```

> **In plain terms** — List your resources in the `try(...)` parentheses and Java closes them for you automatically the instant the block ends — success or failure — in reverse order of opening. No `finally`, no null checks, no forgotten `close()`. Any class that implements `AutoCloseable` (files, streams, connections) works this way.

> **Going deeper** — This solves a subtle bug the manual version gets wrong: if the *body* throws and then `close()` *also* throws, the manual `finally` loses the original exception. Try-with-resources keeps the body's exception primary and attaches the close failure as a *suppressed* exception (retrievable via `Throwable.getSuppressed()`) — so you never lose the root cause. Since Java 9 you can even reference an already-declared effectively-final resource variable directly in the `try(...)`. Always prefer try-with-resources over manual close; it's shorter *and* more correct.

---

## Creating Custom Exceptions

```java
// Unchecked custom exception (most common in service layers)
public class UserNotFoundException extends RuntimeException {
    private final String userId;

    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }

    // Always add a cause-chaining constructor
    public UserNotFoundException(String userId, Throwable cause) {
        super("User not found: " + userId, cause);
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}

// Checked custom exception (for recoverable situations)
public class InsufficientFundsException extends Exception {
    private final double shortfall;

    public InsufficientFundsException(double shortfall) {
        super("Insufficient funds — need " + shortfall + " more");
        this.shortfall = shortfall;
    }

    public double getShortfall() { return shortfall; }
}
```

> **In plain terms** — Build your own exception types when a generic one doesn't tell the full story. `UserNotFoundException` is instantly clearer than `RuntimeException`, and it can carry extra data (the `userId`, the `shortfall`) that a handler needs to react properly. Extend `RuntimeException` for "caller probably can't recover" and `Exception` for "caller should handle this."

> **Going deeper** — Always provide the `(message, Throwable cause)` constructor and pass the cause up via `super(..., cause)` — dropping the cause is the #1 way teams lose the real root error. Carrying typed fields (not just a string) lets handlers branch on data instead of parsing messages. Keep your exception hierarchy shallow and meaningful — a handful of domain exceptions beats one per error. If your custom exception will be thrown in hot paths and you don't need the stack trace, the `(message, cause, enableSuppression, writableStackTrace)` super-constructor lets you disable trace capture, which is the expensive part of `throw`.

---

## Multi-Catch and Re-throw

```java
// Multi-catch: handle multiple exception types the same way
try {
    riskyOperation();
} catch (IOException | SQLException e) {
    logger.error("I/O or DB error", e);
    throw new ServiceException("External error", e);
}

// Re-throw with wrapping (preserves the original cause):
try {
    thirdPartyLib.doSomething();
} catch (ThirdPartyException e) {
    throw new MyAppException("Third-party call failed", e); // e is the cause
}

// The cause chain is visible in the stack trace:
// MyAppException: Third-party call failed
//   Caused by: ThirdPartyException: ...
```

> **In plain terms** — *Multi-catch* (`catch (A | B e)`) lets you handle several unrelated exception types with one block instead of copy-pasting. *Wrapping* (re-throwing as your own exception while passing the original as the `cause`) translates a low-level error into something meaningful for your layer — without throwing away the details of what actually broke.

> **Going deeper** — The `Caused by:` chain in stack traces is your most valuable debugging tool — read it bottom-up to find the true origin, then up to see how it propagated. The variable in a multi-catch is implicitly `final` and typed as the common supertype. Distinguish *wrapping* (new type, cause attached — translate across a boundary) from *rethrowing* (`catch (X e) { log(e); throw e; }` — observe then pass along unchanged). Avoid double-logging: log a wrapped exception once, at the boundary where you finally handle it, not at every layer it passes through.

---

## Exception Best Practices

```java
// 1. NEVER swallow exceptions silently — they disappear forever
try {
    doSomething();
} catch (Exception e) {} // TERRIBLE

// 2. Log the full exception object, not just the message
logger.error("Failed to process", e);            // includes stack trace ✓
logger.error("Failed: " + e.getMessage());        // loses the stack trace ✗

// 3. Catch specific before general
try {
    ...
} catch (FileNotFoundException e) { // specific first
    handleMissingFile();
} catch (IOException e) { // general after
    handleIOError();
}

// 4. Validate early (throw at the top of methods)
public void transfer(Account from, Account to, double amount) {
    if (from == null || to == null) throw new IllegalArgumentException("Accounts can't be null");
    if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
    if (from.equals(to)) throw new IllegalArgumentException("Can't transfer to same account");
    // actual logic starts here, clean
}

// 5. Don't use exceptions for flow control
// BAD:
try {
    int value = Integer.parseInt(input);
} catch (NumberFormatException e) {
    value = 0; // using exception as an if statement
}
// GOOD:
int value = input.matches("\\d+") ? Integer.parseInt(input) : 0;
```

> **In plain terms** — The throughline of all these rules: never make a failure disappear. Don't swallow exceptions, log the whole exception object (so you keep the stack trace), catch specific types before general ones, validate inputs up front so the real logic stays clean, and don't use try/catch as a disguised `if`.

> **Going deeper** — "Don't use exceptions for flow control" has a real cost behind it: constructing an exception captures a stack trace, which is *far* slower than a normal branch — fine for genuine errors, terrible in a tight loop. Catch-specific-first isn't just style: Java actually rejects an unreachable broader-then-narrower order as a compile error. "Validate early" (guard clauses) pairs with `Objects.requireNonNull` and the fail-fast principle — surface bad state at the boundary, close to the cause, rather than letting it corrupt things and blow up somewhere distant.

---

## NullPointerException — Diagnosing the Fix

Java 14+ gives helpful NPE messages:

```
NullPointerException: Cannot invoke "String.length()" because "username" is null
```

```java
// Before fixing, understand WHY it's null:
String username = user.getUsername(); // is user null? is getUsername() returning null?

// Strategies:
// 1. Validate inputs early (best for public methods)
if (username == null) throw new IllegalArgumentException("Username is required");

// 2. Use Optional for values that are legitimately absent
Optional<String> username = Optional.ofNullable(user.getUsername());

// 3. Objects.requireNonNull for fast-fail with a clear message
this.name = Objects.requireNonNull(name, "name must not be null");
```

> **In plain terms** — The NPE is the most common Java crash, and modern Java finally tells you *exactly* which variable was null ("Helpful NullPointerExceptions," on by default since Java 15). The fix is rarely "add a null check at the crash site" — it's understanding *why* something was null and stopping it at the source: validate inputs, return [`Optional`](../04-java8-modern/03-optional.md) instead of null, or fail fast with `requireNonNull`.

> **Going deeper** — Best practice is to make null *impossible* rather than *handled*: never return null from methods (return empty collections or `Optional`), use `requireNonNull` in constructors so objects can't be built in a null state, and prefer immutable types so a field can't become null later. Beware autoboxing NPEs — unboxing a null `Integer` into an `int` throws with no obvious null in sight. Tools help: `@Nullable`/`@NonNull` annotations plus a static analyzer (or an IDE's null analysis) catch many NPEs at compile time, before they ever run.
