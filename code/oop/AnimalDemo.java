public class AnimalDemo {

    // Base class
    static abstract class Animal {
        protected final String name;
        protected int energy;

        public Animal(String name, int energy) {
            this.name = name;
            this.energy = energy;
        }

        // Abstract: must be implemented by subclasses
        public abstract String speak();

        // Concrete: shared by all subclasses
        public void eat(int amount) {
            energy += amount;
            System.out.println(name + " eats, energy now: " + energy);
        }

        // final: no subclass can override this
        public final String identify() {
            return getClass().getSimpleName() + " named " + name;
        }

        @Override
        public String toString() {
            return identify() + ", energy=" + energy;
        }
    }

    static class Dog extends Animal {
        private final String breed;
        private boolean trained;

        public Dog(String name, String breed) {
            super(name, 100); // call parent constructor
            this.breed = breed;
        }

        @Override
        public String speak() {
            return name + " says: Woof! Woof!";
        }

        @Override // use @Override always — compiler catches typos
        public void eat(int amount) {
            super.eat(amount); // call parent's eat
            if (energy > 150) {
                System.out.println(name + " has so much energy, they want to run!");
            }
        }

        public void fetch() {
            if (energy < 20) {
                System.out.println(name + " is too tired to fetch");
                return;
            }
            energy -= 10;
            System.out.println(name + " fetches the ball!");
        }

        public String getBreed() { return breed; }
    }

    static class Cat extends Animal {
        private boolean isIndoor;

        public Cat(String name, boolean isIndoor) {
            super(name, 80);
            this.isIndoor = isIndoor;
        }

        @Override
        public String speak() {
            return name + " says: Meow~";
        }

        public void purr() {
            System.out.println(name + " purrs contentedly");
        }
    }

    static class Bird extends Animal {
        private double wingspan;

        public Bird(String name, double wingspan) {
            super(name, 60);
            this.wingspan = wingspan;
        }

        @Override
        public String speak() {
            return name + " says: Tweet tweet!";
        }

        public void fly() {
            energy -= 15;
            System.out.println(name + " flies with wingspan " + wingspan + "m");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Polymorphism ===");

        // Array of Animal — holds Dog, Cat, Bird
        Animal[] animals = {
            new Dog("Rex", "Labrador"),
            new Cat("Whiskers", true),
            new Dog("Buddy", "Poodle"),
            new Bird("Tweety", 0.3)
        };

        // Same call, different behavior for each type — dynamic dispatch
        for (Animal a : animals) {
            System.out.println(a.speak());
        }

        System.out.println();
        System.out.println("=== instanceof and Pattern Matching ===");

        for (Animal a : animals) {
            // Java 16+ pattern matching: check + cast + bind in one
            if (a instanceof Dog d) {
                System.out.printf("Dog '%s' (breed: %s)%n", d.name, d.getBreed());
                d.fetch();
            } else if (a instanceof Cat c) {
                c.purr();
            } else if (a instanceof Bird b) {
                b.fly();
            }
        }

        System.out.println();
        System.out.println("=== Method Overriding ===");
        Dog rex = new Dog("Rex", "Labrador");
        rex.eat(80); // Dog's overridden eat() with extra logic
    }
}
