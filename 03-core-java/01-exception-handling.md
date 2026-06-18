# Exception Handling

> Runnable example: [code/core/ExceptionDemo.java](../code/core/ExceptionDemo.java)

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
