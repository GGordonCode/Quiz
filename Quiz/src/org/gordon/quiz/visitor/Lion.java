package org.gordon.quiz.visitor;

public class Lion extends Animal {
    /**
     * Constructs a lion with the given name.
     * @param name the name
     */
    public Lion(String name) {
        super(name);
    }

    @Override
    void eat() {
       System.out.println("That gazelle over there is history ... <content too graphic>");
    }

    @Override
    void vocalize() {
        System.out.println("Roar!");
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
        return String.format("lion: '%s'", name);
    }
}
