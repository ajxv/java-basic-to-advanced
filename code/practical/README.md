# Practical — Running the Testing Examples

Unlike the other `code/` examples, the testing snippets in
[06-practical/01-testing.md](../../06-practical/01-testing.md) use **JUnit 5** and
**Mockito**. These are external libraries, so they can't be run with plain
`javac` / `java` the way the other demos can — you need a build tool to fetch the
dependencies and run the test engine.

## Maven

```xml
<!-- pom.xml -->
<dependencies>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

```bash
mvn test
```

## Gradle

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}
tasks.test { useJUnitPlatform() }
```

```bash
./gradlew test
```

## Where the test subjects live

The testing guide writes tests against classes like `BankAccount`, which already
exists as a runnable demo here:

- [../oop/BankAccountDemo.java](../oop/BankAccountDemo.java)

Copy that class into a project set up as above, drop the test classes from the
guide under `src/test/java`, and run `mvn test` / `./gradlew test`.
