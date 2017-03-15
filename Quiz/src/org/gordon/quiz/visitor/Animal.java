package org.gordon.quiz.visitor;

/**
 * The base class of Animals for the purpose of demonstrating the Visitor pattern,
 * where we will add additional animal-related functionality.  We declare that the
 * abstract class implements the visitable interface, even though the implementation
 * of the interface must come from each class extending this class.  Declaring the
 * "implements" here, will force all future sublcasses to declare it.
 * @author Gary
 *
 */

public abstract class Animal implements AnimalVisitable {
    
    // We'll assume we can even name wild animals.  I mean we have names for the
    // geckos in our yard, so ...
    protected String name;
    
    protected Animal(String name) {
        this.name = name;
    }

    /**
     * Get the animal's name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Consume consumables.
     */
    abstract void eat();
    
    /**
     * Make sounds.
     */
    abstract void vocalize();
}
