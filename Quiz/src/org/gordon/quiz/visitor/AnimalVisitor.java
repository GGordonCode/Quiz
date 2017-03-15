package org.gordon.quiz.visitor;

/**
 * As part ot the Visitor pattern, we define generic "visit" methods for each type.  Note
 * Java will automatically bind in the correct method to match the proper one when "this"
 * is passed as a parameter.
 * @author Gary
 *
 */

public interface AnimalVisitor {

    /**
     * Visit a cat to do something with it.
     * @param cat the cat to visit
     */
    void visit(Cat cat);
    
    /**
     * Visit a dog to do something with it.
     * @param dog the dog to visit
     */
    void visit(Dog dog);
    
    /**
     * Visit a lion to do something with it.
     * @param lion the lion to visit
     */
    void visit(Lion lion);
}
