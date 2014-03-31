package edu.buffalo.cse.jive.model;

/**
 * This type encapsulates one or more workspace projects supported by Jive. This is necessary in
 * order to reconcile the differences among the types implemented by each nature (e.g., Java, WST,
 * PDE, etc). This type provides access to the actual sources, when available.
 */
public interface IJiveProject
{
  public static final String JIVE_PROJECT_INSTANCE = "jive.project.instance";

  public void addProject(Object project);

  public Object findType(String typeName);

  public Object launch();

  public String name();

  public int targetId();
}