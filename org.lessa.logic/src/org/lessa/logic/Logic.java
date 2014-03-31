package org.lessa.logic;

public interface Logic
{
  /**
   * Formula
   */
  public interface IFormula
  {
    public ISubformula getFormula();
  }

  /**
   * Subformula
   */
  public interface ISubformula
  {
  }

  /**
   * Subformula::Atom
   */
  public interface IAtom extends ISubformula
  {
  }

  /**
   * Subformula::Atom::Boolean
   */
  public interface IBooleanAtom extends IAtom
  {
    public IBooleanConstant getConstant();
  }

  /**
   * Subformula::Atom::Predicate
   */
  public interface IPredcateAtom extends IAtom
  {
    public IPredicate getPredicate();
  }

  /**
   * Subformula::QuantifiedFormula
   */
  public interface IQuantifiedFormula extends ISubformula
  {
    public IQuantifier getQuantifier();

    public IVariable getVariable();

    public ISubformula getFormula();
  }

  /**
   * Quantifier
   */
  public interface IQuantifier
  {
  }

  /**
   * Quantifier::ExistentialQuantifier
   */
  public interface IExistentialQuantifier extends IQuantifier
  {
  }

  /**
   * Quantifier::UniversalQuantifier
   */
  public interface IUniversalQuantifier extends IQuantifier
  {
  }

  /**
   * Connective
   */
  public interface IConnective
  {
  }

  /**
   * Connective::Conjunctive
   */
  public interface IConjunctiveConnective extends IConnective
  {
  }

  /**
   * Subformula::IConnectedFormula
   */
  public interface IConnectedFormula extends ISubformula
  {
    public IConnective getConnective();

    public ISubformula getLeftFormula();

    public ISubformula getRightFormula();
  }

  /**
   * Predicate: D1 x ... x Dn --> boolean
   */
  public interface IPredicate
  {
    public Integer getArity();

    public String getName();

    public ITerm getTerm(final int position);
  }

  /**
   * Predicate::Equals
   */
  public interface IEquals extends IPredicate
  {
  }

  /**
   * Predicate::GreaterOrEqualTo
   */
  public interface IGreaterOrEqualTo extends IPredicate
  {
  }

  /**
   * Predicate::GreaterThan
   */
  public interface IGreaterThan extends IPredicate
  {
  }

  /**
   * Predicate::LessOrEqualTo
   */
  public interface ILessOrEqualTo extends IPredicate
  {
  }

  /**
   * Predicate::LessThan
   */
  public interface ILessThan extends IPredicate
  {
  }

  /**
   * Term
   */
  public interface ITerm
  {
  }

  /**
   * Term::Constant
   */
  public interface IConstant extends ITerm
  {
    public Object getValue();
  }

  /**
   * Term::Constant::BooleanConstant
   */
  public interface IBooleanConstant extends IConstant
  {
    @Override
    public Boolean getValue();
  }

  /**
   * Term::Constant::IntegerConstant
   */
  public interface IIntegerConstant extends IConstant
  {
    @Override
    public Integer getValue();
  }

  /**
   * Term::Constant::RealConstant
   */
  public interface IRealConstant extends IConstant
  {
    @Override
    public Double getValue();
  }

  /**
   * Term::Constant::StringConstant
   */
  public interface IStringConstant extends IConstant
  {
    @Override
    public String getValue();
  }

  /**
   * Term::Function: D1 x ... x Dn --> I
   */
  public interface IFunction extends ITerm
  {
    public Integer getArity();

    public String getName();

    public ITerm getTerm(final int position);
  }

  /**
   * Term::Function::Add
   */
  public interface IAdd extends IFunction
  {
  }

  /**
   * Term::Variable
   */
  public interface IVariable extends ITerm
  {
    public String getName();
  }
}
