package org.gordon.quiz.visitor;

/**
 * In this class we add the desired functionality (here to train an animal)
 * to the various Animal types in this external interface.  A situation where
 * this would come in handy is where you don't have the ability to modify the
 * existing classes.  
 * @author Gary
 *
 */

public class AnimalTrainerVisitor implements AnimalVisitor {

    @Override
    public void visit(Cat cat) {
        System.out.println(String.format("Training cat '%s' ...", cat.getName()));
        System.out.println("OK, kitty, here's your litter box, now let me explain ...");
        System.out.println();
    }

    @Override
    public void visit(Dog dog) {
        System.out.println(String.format("Training dog '%s' ...", dog.getName()));
        System.out.println("OK, pooch I'm getting the leash, you know what that means!");
        System.out.println();
    }

    @Override
    public void visit(Lion lion) {
        System.out.println(String.format("Training lion '%s' ...", lion.getName()));
        System.out.println("Nice fella, here's a slab of raw meat, enjoy your 'mane' course!");
        System.out.println();
    }

}
