package edu.buffalo.cse.jive.model;

public interface IVisitor<E>
{
  public void visit(E element);
}