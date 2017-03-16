package org.gordon.quiz.visitor;

public class AnimalGroomerVisitor implements AnimalVisitor {

    @Override
    public void visit(Cat cat) {
        System.out.println(String.format("Grooming cat '%s' ...", cat.getName()));
        System.out.println("OK kitty, here's that spiky brush you like ...");
        System.out.println();
    }

    @Override
    public void visit(Dog dog) {
        System.out.println(String.format("Grooming dog '%s' ...", dog.getName()));
        System.out.println("OK pooch, time for the bathtub, your favorite ...");
        System.out.println();
    }

    @Override
    public void visit(Lion lion) {
        System.out.println(String.format("Grooming lion '%s' ...", lion.getName()));
        System.out.println("Hmmm, do I have a 50 ft. long brush, that's close enough!");
        System.out.println();
    }

}
