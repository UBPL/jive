package org.lessa.logic;

import org.lessa.logic.Logic.IAtom;
import org.lessa.logic.Logic.IFormula;

public class LogicFactory
{
  public interface ILogicFormulaBuilder
  {
  }

  private static class LogicFormulaBuilder implements ILogicFormulaBuilder
  {
    public IFormula create()
    {
      return null;
    }

    public LogicFormulaBuilder addAtom(final IAtom atom)
    {
    }
  }

  public static LogicFormulaBuilder createEmpty()
  {
    return new LogicFormulaBuilder();
  }

  public static LogicFormulaBuilder createAtomic(final boolean atom)
  {
    return new LogicFormulaBuilder(atom);
  }
}
