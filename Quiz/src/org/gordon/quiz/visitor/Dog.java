package org.gordon.quiz.visitor;

public class Dog extends Animal {
    
    /**
     * Constructs a dog with the given name.
     * @param name the name
     */
    public Dog(String name) {
        super(name);
    }

    @Override
    void eat() {
        System.out.println("Chomp, chomp, love that Alpo!");
    }

    @Override
    void vocalize() {
        System.out.println("Woof!");
    }
    
    /**
     * 
     * @param visitor
     */
    public void accept(AnimalVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.format("dog: '%s'", name);
    }
}
