package edu.buffalo.cse.jive.lib;

public class XMLTools
{
  public static enum XMLEventField
  {
    CALLER("caller"),
    CATCHER("catcher"),
    CONTEXT("context"),
    ELEMENTS("elements"),
    EXCEPTION("exception"),
    FIELD("field"),
    FILE("file"),
    FRAME_POPPED("framePopped"),
    ID("id"),
    IMMORTAL("immortal"),
    INLINE("inline"),
    KIND("kind"),
    LINE("line"),
    LOCK("lock"),
    MONITOR("monitor"),
    NEWTHREAD("newthread"),
    OBJECT("object"),
    OPERATION("operation"),
    PRIORITY("priority"),
    RETURNER("returner"),
    SCHEDULER("scheduler"),
    SCOPE("scope"),
    SIZE("size"),
    SIGNATURE("signature"),
    TARGET("target"),
    THREAD("thread"),
    THROWER("thrower"),
    TIMESTAMP("timestamp"),
    TYPE("type"),
    VALUE("value"),
    VARIABLE("variable"),
    WAKETIME("waketime");
    private final String name;

    private XMLEventField(final String name)
    {
      this.name = name;
    }

    public String fieldName()
    {
      return name;
    }

    @Override
    public String toString()
    {
      return name;
    }
  }
}