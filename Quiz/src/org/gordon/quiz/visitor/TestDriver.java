package org.gordon.quiz.visitor;

import java.util.ArrayList;
import java.util.List;

public class TestDriver {
    public static void main(String[] args) {
        List<Animal> animals = new ArrayList<>();
        animals.add(new Dog("Spot"));
        animals.add(new Cat("Mittens"));
        animals.add(new Lion("Sir"));
        animals.add(new Dog("Fido"));
        AnimalTrainerVisitor trainer = new AnimalTrainerVisitor();
        System.out.println("Doing things with the animals ...");
        System.out.println();
        for (Animal a: animals) {
            System.out.println("Next up, " + a);
            a.vocalize();
            a.eat();
            a.accept(trainer);
        }
    }
}
