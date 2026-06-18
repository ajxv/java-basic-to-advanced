import java.util.Objects;

public class BankAccountDemo {

    // Encapsulated class — state only accessible through controlled methods
    static class BankAccount {
        private final String accountId;
        private double balance;
        private int transactionCount;

        public BankAccount(String accountId, double initialBalance) {
            if (initialBalance < 0)
                throw new IllegalArgumentException("Initial balance cannot be negative");
            this.accountId = Objects.requireNonNull(accountId, "accountId required");
            this.balance = initialBalance;
            this.transactionCount = 0;
        }

        // Copy constructor
        public BankAccount(BankAccount other) {
            this(other.accountId + "-COPY", other.balance);
            this.transactionCount = other.transactionCount;
        }

        public void deposit(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive, got: " + amount);
            this.balance += amount;
            this.transactionCount++;
            System.out.printf("[%s] Deposit %.2f → Balance: %.2f%n", accountId, amount, balance);
        }

        public void withdraw(double amount) {
            if (amount <= 0) throw new IllegalArgumentException("Withdrawal must be positive");
            if (amount > balance) throw new IllegalStateException(
                String.format("Insufficient funds: balance=%.2f, requested=%.2f", balance, amount));
            this.balance -= amount;
            this.transactionCount++;
            System.out.printf("[%s] Withdraw %.2f → Balance: %.2f%n", accountId, amount, balance);
        }

        public double getBalance() { return balance; }
        public String getAccountId() { return accountId; }
        public int getTransactionCount() { return transactionCount; }

        @Override
        public String toString() {
            return String.format("BankAccount{id='%s', balance=%.2f, txns=%d}",
                accountId, balance, transactionCount);
        }
    }

    // Demonstrate `this` and `static`
    static class Employee {
        private static int totalEmployees = 0;   // ONE copy shared by all instances
        private final int employeeId;
        private final String name;
        private double salary;

        public Employee(String name, double salary) {
            totalEmployees++;
            this.employeeId = totalEmployees;
            this.name = Objects.requireNonNull(name, "name");
            this.salary = salary;
        }

        public void raiseSalary(double percent) {
            double raise = this.salary * percent / 100;
            this.salary += raise;
            System.out.printf("%s received %.1f%% raise → new salary: %.0f%n", name, percent, salary);
        }

        public static int getTotalEmployees() { return totalEmployees; } // no `this`
        public String getName() { return name; }
        public double getSalary() { return salary; }

        @Override
        public String toString() {
            return String.format("Employee{id=%d, name='%s', salary=%.0f}", employeeId, name, salary);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== BankAccount — Encapsulation ===");
        BankAccount acc = new BankAccount("ACC-001", 1000.00);
        acc.deposit(500.00);
        acc.withdraw(200.00);
        System.out.println("Final: " + acc);

        // Copy constructor
        BankAccount copy = new BankAccount(acc);
        System.out.println("Copy: " + copy);
        copy.deposit(100.00); // copy is independent
        System.out.println("Original after modifying copy: " + acc.getBalance()); // unchanged

        System.out.println();
        System.out.println("=== Exception Handling in BankAccount ===");
        try {
            acc.withdraw(99999.00);
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        try {
            acc.deposit(-50);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== Employee — static vs instance ===");
        System.out.println("Total employees: " + Employee.getTotalEmployees());
        Employee e1 = new Employee("Alice", 60000);
        Employee e2 = new Employee("Bob", 75000);
        Employee e3 = new Employee("Charlie", 80000);
        System.out.println("Total employees: " + Employee.getTotalEmployees());
        e1.raiseSalary(10);
        System.out.println(e1);
        System.out.println(e2);
    }
}
