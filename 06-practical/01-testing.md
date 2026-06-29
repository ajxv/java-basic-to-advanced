# Testing with JUnit 5 and Mockito

> Setup & how to run these tests: [code/practical/README.md](../code/practical/README.md) (JUnit/Mockito need a build tool, not plain `javac`)

---

## The Big Picture

> **In plain terms** — A test is code that runs your code and checks it did the right thing — automatically. Instead of manually clicking through the app to see if a change broke something, you write small programs that assert "given this input, the result should be that." **JUnit** is the framework that finds and runs your tests; **Mockito** lets you replace real dependencies (databases, APIs) with controllable fakes so you can test one piece in isolation.

> **Why this matters** — Tests are what let you *change code without fear*. A solid test suite catches regressions the instant you introduce them, documents how the code is meant to behave, and lets you refactor aggressively because the tests will scream if you break something. In a job, untested code is code nobody dares touch. The core skills here — clear test structure (Arrange-Act-Assert), good assertions, and mocking dependencies — are what separate "I think it works" from "I can prove it works."

---

## Why Tests Matter

Tests are how you prove your code works, catch regressions, and refactor safely. In a job setting, untested code is code you're afraid to change.

> **In plain terms** — Think of tests as a safety net you build once and benefit from forever. Every bug you fix becomes a test that ensures it never comes back. Every feature you add gets a test that proves it works and keeps working. The upfront cost pays for itself the first time a test catches a mistake before it reaches users.

> **Going deeper** — The practical guideline is the *test pyramid*: lots of fast unit tests (one class, mocked dependencies), fewer integration tests (several real components together), and a handful of slow end-to-end tests. Aim for tests that are *fast, isolated, repeatable, and deterministic* — a flaky test (passes sometimes, fails others) is worse than no test because it erodes trust. Coverage percentage is a weak proxy: 100% coverage of trivial getters proves little, while well-chosen tests of edge cases and business rules prove a lot. Some teams write the test *first* (TDD) to clarify the desired behavior before implementing.

---

## JUnit 5 Basics

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    private BankAccount account;

    @BeforeEach // runs before each test — fresh state
    void setUp() {
        account = new BankAccount("ACC-001", 1000.0);
    }

    @AfterEach // cleanup if needed
    void tearDown() {}

    @BeforeAll  // runs once before all tests in this class (must be static)
    static void init() {}

    @Test
    void deposit_shouldIncreaseBalance() {
        // Arrange (given)
        double depositAmount = 500.0;

        // Act (when)
        account.deposit(depositAmount);

        // Assert (then)
        assertEquals(1500.0, account.getBalance(), 0.001); // delta for doubles
    }

    @Test
    void withdraw_withInsufficientFunds_shouldThrowException() {
        assertThrows(IllegalStateException.class,
            () -> account.withdraw(5000.0)); // expects this to throw
    }

    @Test
    void withdraw_exactBalance_shouldSucceed() {
        account.withdraw(1000.0);
        assertEquals(0.0, account.getBalance(), 0.001);
    }

    @Test
    @Disabled("Skipped until payment gateway is configured")
    void testPaymentGateway() { }
}
```

> **In plain terms** — JUnit's annotations mark what's what: `@Test` is a test, `@BeforeEach` runs setup before *every* test (a fresh object so tests don't pollute each other), `@AfterEach` cleans up, and `@BeforeAll`/`@AfterAll` run once for the whole class. Each test typically follows *Arrange-Act-Assert*: set up the inputs, call the method, then assert the result.

> **Going deeper** — The `@BeforeEach` fresh-state habit is crucial: tests must be *independent* and *order-independent* (JUnit doesn't guarantee order), so shared mutable state between tests causes mysterious failures. `@BeforeAll`/`@AfterAll` must be `static` because they run before any instance exists — JUnit creates a new test-class instance per test method by default, exactly to enforce isolation. Prefer `assertThrows` over try/catch for expected exceptions, name tests for behavior (`withdraw_withInsufficientFunds_shouldThrow`), and use `@Disabled` with a *reason* rather than commenting tests out (so they stay visible and counted).

---

## All Assertion Types

```java
// Equality
assertEquals(expected, actual);
assertEquals(3.14, result, 0.001); // delta for doubles
assertNotEquals("hello", result);

// Boolean
assertTrue(condition);
assertFalse(condition);

// Null checks
assertNull(value);
assertNotNull(value);

// Same object (reference)
assertSame(expected, actual);

// Array contents
assertArrayEquals(new int[]{1,2,3}, result);

// Exceptions
assertThrows(IllegalArgumentException.class, () -> method(badInput));
// Or capture the exception to check its message:
IllegalArgumentException ex = assertThrows(
    IllegalArgumentException.class, () -> method(badInput));
assertTrue(ex.getMessage().contains("must be positive"));

// Multiple assertions: all run even if one fails
assertAll("user fields",
    () -> assertEquals("Alice", user.getName()),
    () -> assertEquals(30, user.getAge()),
    () -> assertTrue(user.isActive())
);

// Timeout
assertTimeout(Duration.ofMillis(100), () -> quickMethod());
```

> **In plain terms** — Assertions are how a test states "this must be true." If the assertion holds, the test passes silently; if not, it fails with a helpful message showing expected vs actual. Beyond `assertEquals`, learn `assertThrows` (the right way to test that something fails), `assertAll` (run several checks and report *all* failures, not just the first), and the float `delta` (never compare doubles exactly).

> **Going deeper** — Test one *behavior* per test, but multiple assertions about that behavior are fine — group them with `assertAll` so a failure in the first doesn't hide the rest. The float `delta` ties back to the [floating-point trap](../01-basics/02-variables-and-types.md#the-floating-point-trap): `assertEquals(0.3, a)` can fail on rounding, so you pass a tolerance. For richer, more readable assertions (especially on collections and objects), many teams prefer AssertJ's fluent style (`assertThat(list).contains(x).hasSize(3)`) over the built-in `assert*`. Capturing the thrown exception (`assertThrows(...)` returns it) lets you assert on its message/fields too.

---

## Parameterized Tests

Run the same test with different inputs — eliminates copy-paste tests.

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class ValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  ", "\t"})
    void isBlank_shouldReturnTrue(String input) {
        assertTrue(input.isBlank());
    }

    @ParameterizedTest
    @CsvSource({
        "alice@email.com, true",
        "not-an-email, false",
        "@nodomain, false",
        "user@, false"
    })
    void isValidEmail(String email, boolean expected) {
        assertEquals(expected, validator.isValidEmail(email));
    }

    @ParameterizedTest
    @MethodSource("provideSalaryTestCases")
    void calculateTax(int salary, double expectedTax) {
        assertEquals(expectedTax, calculator.calculateTax(salary), 0.01);
    }

    static Stream<Arguments> provideSalaryTestCases() {
        return Stream.of(
            Arguments.of(30_000, 3_000.0),   // 10%
            Arguments.of(50_000, 7_500.0),   // 15%
            Arguments.of(100_000, 20_000.0)  // 20%
        );
    }
}
```

> **In plain terms** — When you want to test the same logic against many inputs, don't copy-paste the test five times. A parameterized test runs once per input set — `@ValueSource` for a list of single values, `@CsvSource` for input→expected pairs, `@MethodSource` when the cases need real objects. One test, many cases, each reported separately.

> **Going deeper** — These shine for *table-driven* testing: enumerate the interesting cases — boundaries (0, max, negative), empty/blank, and known tricky values — in one place. `@CsvSource` is perfect for "input, expected" rows; `@MethodSource` handles cases that need constructed objects or computed values. Each invocation is an independent test, so one failing input doesn't stop the others, and the failure report tells you exactly which row broke. This pairs naturally with thinking about *equivalence classes and edge cases* rather than just the happy path.

---

## Mockito — Mocking Dependencies

Mocks replace real dependencies (databases, APIs, services) with controlled fakes.

> **In plain terms** — To test a `UserService` in isolation, you don't want to hit a real database — it's slow, flaky, and hard to set up. A *mock* is a fake stand-in: you tell it "when `findById("999")` is called, return empty" (`when(...).thenReturn(...)`), then check afterward that your code called what it should (`verify(...)`). This lets you test your logic, and only your logic, against any scenario you can imagine.

> **Going deeper** — Mocks need a seam to plug into — this is *why* you [depend on interfaces and inject dependencies](../02-oop/03-interfaces-and-abstract-classes.md): `@InjectMocks` builds the service with `@Mock`s wired in. Two distinct purposes: *stubbing* (`when/thenReturn`) controls inputs your code receives; *verification* (`verify`) checks outputs your code produces (the calls it made). Don't over-verify — asserting every interaction makes tests brittle and couples them to implementation; verify only the interactions that matter to the behavior. And a caution: if you find yourself mocking deep chains or value objects, prefer a real instance or a simple fake — mock at the *boundaries* (repositories, external services), not everywhere.

```java
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class) // wires up mocks automatically
class UserServiceTest {

    @Mock
    UserRepository userRepo; // fake repository — no actual DB

    @Mock
    EmailService emailService; // fake email service

    @InjectMocks
    UserService userService; // system under test — mocks injected automatically

    @Test
    void createUser_shouldSaveAndSendWelcomeEmail() {
        // Arrange: stub the repository
        User savedUser = new User("123", "Alice", "alice@email.com");
        when(userRepo.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.createUser("Alice", "alice@email.com");

        // Assert result
        assertNotNull(result);
        assertEquals("Alice", result.getName());

        // Verify interactions
        verify(userRepo).save(any(User.class));          // save was called once
        verify(emailService).sendWelcome("alice@email.com"); // email was sent
        verifyNoMoreInteractions(emailService);           // no other emails sent
    }

    @Test
    void getUser_whenNotFound_shouldThrowException() {
        when(userRepo.findById("999")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
            () -> userService.getUser("999"));

        verify(userRepo).findById("999");
    }

    @Test
    void getUser_whenRepoThrows_shouldPropagateException() {
        when(userRepo.findById("bad")).thenThrow(new RuntimeException("DB down"));

        assertThrows(RuntimeException.class, () -> userService.getUser("bad"));
    }
}
```

> **In plain terms** — These tests show the full pattern: `@ExtendWith(MockitoExtension.class)` turns on Mockito, `@Mock` makes the fakes, `@InjectMocks` builds the real object-under-test with those fakes inside, and each test stubs the scenario then asserts the outcome — including failure scenarios like "repository returns empty" or "repository throws," which would be painful to reproduce with a real database.

> **Going deeper** — Notice how easy it is to test the *unhappy paths* (not found, dependency throws) — that's mocking's biggest payoff, since those are exactly the cases hardest to trigger with real systems yet most important to get right. `MockitoExtension` also enforces *strict stubbing*: an unused `when(...)` becomes a test failure, catching copy-paste leftovers. For methods returning `void` you stub differently (`doThrow().when(mock).method()`). When a class has too many mocks to wire up, that's design feedback — it likely has too many dependencies and wants splitting.

---

## Argument Captors — Verify What Was Passed

```java
@Captor
ArgumentCaptor<User> userCaptor;

@Test
void createUser_shouldSaveWithCorrectData() {
    userService.createUser("Alice", "alice@email.com");

    verify(userRepo).save(userCaptor.capture());
    User captured = userCaptor.getValue();

    assertEquals("Alice", captured.getName());
    assertEquals("alice@email.com", captured.getEmail());
    assertNotNull(captured.getCreatedAt()); // check auto-generated field
}
```

> **In plain terms** — Sometimes you don't just want to know *that* a dependency was called — you want to inspect *what* it was called with. An `ArgumentCaptor` grabs the actual object passed to a mock so you can assert on its contents, like checking that the `User` handed to `save()` had the right name and a generated timestamp.

> **Going deeper** — Captors are the tool for verifying objects your code *constructs internally* and passes on — things you can't see from the return value. Use them when an exact-match argument matcher would be too rigid (e.g. you only care about two of five fields, or a field is generated). For simple cases, an inline matcher (`verify(repo).save(argThat(u -> u.getName().equals("Alice")))`) reads fine; reach for a captor when you need to assert several things about the captured value. Don't capture what you don't assert on — it adds noise.

---

## Test Structure: Keep Tests Clear

```java
// Name: methodName_condition_expectedBehavior
void deposit_withNegativeAmount_shouldThrowIllegalArgument()
void getUser_whenActive_shouldReturnUser()
void calculateTax_forHighIncome_shouldApplyHighestBracket()

// Structure: Arrange → Act → Assert (AAA)
// Or: Given → When → Then (BDD style — same thing)

// ONE assertion concept per test (multiple assertEquals is fine if they test the same thing)
// Don't test multiple behaviors in one test — split them
```

> **In plain terms** — A test's name should tell you what broke without opening the code: `deposit_withNegativeAmount_shouldThrow` says it all. Keep each test focused on one behavior and structured as Arrange-Act-Assert, so when it fails the reason is obvious. A failing test should point straight at the problem, not send you on an investigation.

> **Going deeper** — Treat test code as *first-class code* — it's read far more than it's written, and a confusing test suite is a liability. The naming convention `method_condition_expectedBehavior` doubles as living documentation of the class's contract. Avoid logic (loops, conditionals) in tests — they should be obvious enough to not need testing themselves. When a single test needs many assertions about *different* behaviors, that's a signal to split it; when setup is enormous, that's design feedback that the class-under-test may be doing too much. Good tests are the executable specification of how your code is supposed to behave.

---

## Integration Tests vs Unit Tests

```java
// Unit test: tests a single class in isolation, mocks all dependencies
// Fast (ms), test business logic

// Integration test: tests multiple real components together
// Slower, test that components work together

// Mark them separately to run them separately:
@Tag("unit")        // @Tag on class or method
@Tag("integration")

// Run only unit tests:
// mvn test -Dgroups=unit
// gradle test --tests "*" -Dtags=unit
```

> **In plain terms** — *Unit tests* check one class in isolation with everything else mocked — fast and precise, the bulk of your suite. *Integration tests* wire up several real components (or a real database) to confirm they actually work together — slower but catch a different class of bug (wrong SQL, mismatched assumptions between layers). Tag them so you can run the fast ones constantly and the slow ones less often.

> **Going deeper** — This is the test pyramid in practice: many unit tests (run on every save/build), fewer integration tests (run in CI), few end-to-end tests. Unit tests catch logic bugs; integration tests catch *wiring* bugs that mocks can hide — a mock returning what you *assume* the real dependency returns can pass while production fails. Modern integration testing often uses *Testcontainers* to spin up a real database/queue in Docker for the test, giving fidelity without a shared test environment. Separating by `@Tag` keeps your fast feedback loop fast (developers run units locally; CI runs everything). Whatever the split, automate it all in CI so every push is verified.
