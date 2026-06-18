# Testing with JUnit 5 and Mockito

> Runnable example: [code/practical/](../code/practical/)

---

## Why Tests Matter

Tests are how you prove your code works, catch regressions, and refactor safely. In a job setting, untested code is code you're afraid to change.

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

---

## Mockito — Mocking Dependencies

Mocks replace real dependencies (databases, APIs, services) with controlled fakes.

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
