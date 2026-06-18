# Java: Basics to Advanced

A practical, job-ready Java guide with runnable code examples.

---

## How to Use This

Each section has two parts:
- A **markdown guide** explaining the concept with inline code
- A **runnable `.java` file** in `code/` you can compile and run right now

```bash
# Compile and run any example:
cd code/basics
javac TypesDemo.java && java TypesDemo
```

---

## Table of Contents

### Part 1 — Basics
| File | What You'll Learn |
|------|-------------------|
| [01-how-java-works.md](01-basics/01-how-java-works.md) | JVM, JDK, JRE, bytecode, JIT |
| [02-variables-and-types.md](01-basics/02-variables-and-types.md) | Primitives, stack vs heap, `==` vs `equals` |
| [03-operators-and-control-flow.md](01-basics/03-operators-and-control-flow.md) | Loops, switch expressions, ternary |
| [04-methods.md](01-basics/04-methods.md) | Pass-by-value, overloading, varargs |

### Part 2 — Object-Oriented Programming
| File | What You'll Learn |
|------|-------------------|
| [01-classes-and-objects.md](02-oop/01-classes-and-objects.md) | Encapsulation, constructors, `this`, `static` |
| [02-inheritance-and-polymorphism.md](02-oop/02-inheritance-and-polymorphism.md) | `extends`, method overriding, dynamic dispatch |
| [03-interfaces-and-abstract-classes.md](02-oop/03-interfaces-and-abstract-classes.md) | When to use which, default methods |

### Part 3 — Core Java
| File | What You'll Learn |
|------|-------------------|
| [01-exception-handling.md](03-core-java/01-exception-handling.md) | Checked vs unchecked, try-with-resources, custom exceptions |
| [02-strings.md](03-core-java/02-strings.md) | Immutability, String Pool, StringBuilder, key methods |
| [03-collections.md](03-core-java/03-collections.md) | List, Set, Map — when to use which |
| [04-generics.md](03-core-java/04-generics.md) | Type safety, wildcards, bounded parameters |

### Part 4 — Java 8 and Modern Java
| File | What You'll Learn |
|------|-------------------|
| [01-lambdas-and-functional.md](04-java8-modern/01-lambdas-and-functional.md) | Lambdas, functional interfaces, method references |
| [02-streams.md](04-java8-modern/02-streams.md) | Stream pipelines, collect, groupingBy, parallel |
| [03-optional.md](04-java8-modern/03-optional.md) | Eliminating NullPointerException |
| [04-modern-java-9-to-21.md](04-java8-modern/04-modern-java-9-to-21.md) | Records, sealed classes, var, virtual threads |

### Part 5 — Advanced
| File | What You'll Learn |
|------|-------------------|
| [01-file-io.md](05-advanced/01-file-io.md) | NIO, buffered I/O, walking directories |
| [02-multithreading.md](05-advanced/02-multithreading.md) | Threads, race conditions, locks, CompletableFuture |
| [03-memory-and-gc.md](05-advanced/03-memory-and-gc.md) | Heap, GC generations, memory leaks |
| [04-design-patterns.md](05-advanced/04-design-patterns.md) | Singleton, Builder, Factory, Strategy, Observer |

### Part 6 — Practical
| File | What You'll Learn |
|------|-------------------|
| [01-testing.md](06-practical/01-testing.md) | JUnit 5, Mockito, parameterized tests |
| [02-performance.md](06-practical/02-performance.md) | Profiling, string performance, JVM flags |
| [03-quick-reference.md](06-practical/03-quick-reference.md) | Decision trees, cheat sheets |

### Runnable Code Examples
All in [code/](code/)

---

## Recommended Learning Path

**Week 1:** Part 1 (Basics) + Part 2 (OOP)  
**Week 2:** Part 3 (Core Java)  
**Week 3:** Part 4 (Java 8 + Modern)  
**Week 4:** Part 5 (Advanced) + Part 6 (Practical)

Run the code examples as you go. The best way to learn is to modify them and see what breaks.
