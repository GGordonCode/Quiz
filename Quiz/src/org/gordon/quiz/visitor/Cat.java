package org.gordon.quiz.visitor;

public class Cat extends Animal {
    
    /**
     * Constructs a cat with the given name.
     * @param name the name
     */
    public Cat(String name) {
        super(name);
    }

    @Override
    void eat() {
       System.out.println("Nine Lives, well, ... OK I'll eat this slop.");
    }

    @Override
    void vocalize() {
        System.out.println("Meow!");
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
        return String.format("cat: '%s'", name);
    }
}
