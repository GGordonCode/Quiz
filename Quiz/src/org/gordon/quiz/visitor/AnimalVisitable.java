package org.gordon.quiz.visitor;

/**
 * Here we define the base accept() method which will be applied polymorphically by
 * each concrete subclass of Animal.
 * @author Gary
 *
 */
public interface AnimalVisitable {
    void accept(AnimalVisitor visitor);
}
