package edu.buffalo.cse.jive.internal.practql.visitor;

import edu.buffalo.cse.jive.practql.expression.ExpressionException;
import edu.buffalo.cse.jive.practql.expression.IExpressionVisitor;
import edu.buffalo.cse.jive.practql.expression.atomic.IAggregateExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFieldExpression;
import edu.buffalo.cse.jive.practql.expression.atomic.IFunctionCallExpression;
import edu.buffalo.cse.jive.practql.expression.literal.ILiteral;
import edu.buffalo.cse.jive.practql.expression.nary.IAddition;
import edu.buffalo.cse.jive.practql.expression.nary.IConjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IDisjunction;
import edu.buffalo.cse.jive.practql.expression.nary.IMultiplication;
import edu.buffalo.cse.jive.practql.expression.relational.IRelationalExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INamedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegatedExpression;
import edu.buffalo.cse.jive.practql.expression.unary.INegativeExpression;
import edu.buffalo.cse.jive.practql.expression.unary.ISortedExpression;

public class DNFVisitor implements IExpressionVisitor
{
  @Override
  public boolean visitAddition(final IAddition expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitAggregate(final IAggregateExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitConjunction(final IConjunction expression, final Object arg)
      throws ExpressionException
  {
    // @Override
    // public Disjunction toDNF() {
    //
    // // (F1 ^ ... ^ FN) = CONVERT(F1) ^ ... ^ CONVERT(FN)
    // // (F1 ^ ... ^ FN) = (F11 v ...) ^ ... ^ (FN1 v ...)
    // // (F1 ^ ... ^ FN) = (F11 ^ F21 ^ ... ^ FN1) v ...
    // final List<Disjunction> ds = new ArrayList<Disjunction>();
    // for (int i = 0; i < conjuncts.size(); i++) {
    // ds.add(conjuncts.get(i).toDNF());
    // }
    // List<Conjunction> answer = new ArrayList<Conjunction>();
    // // initialize the list of conjuncts with the disjuncts from the first disjunction
    // for (int i = 0; i < ds.get(0).size(); i++) {
    // answer.add(new ConjunctionImpl(ds.get(0).getDisjunct(i)));
    // }
    // // "join" each disjunct with every disjunct in the other disjunctions
    // for (int i = 1; i < ds.size(); i++) {
    // final List<Conjunction> current = new ArrayList<Conjunction>();
    // // traverse the conjunctions in the current answer
    // for (int k = 0; k < answer.size(); k++) {
    // // combine each disjunct in the current disjunction with the answer set
    // // this increases the length of the answer: N1 x N2 x ... x Nm
    // for (int j = 0; j < ds.get(i).size(); j++) {
    // // copy constructor
    // final Conjunction c = new ConjunctionImpl(answer.get(k));
    // // append the current disjunct to the answer
    // c.append(ds.get(i).getDisjunct(j));
    // // add to the current result
    // current.add(c);
    // }
    // }
    // answer = current;
    // }
    // // put the disjunction together
    // final Disjunction d = new DisjunctionImpl(answer.get(0));
    // for (int i = 1; i < answer.size(); i++) {
    // d.append(answer.get(i));
    // }
    // return d;
    // }
    return false;
  }

  @Override
  public boolean visitDisjunction(final IDisjunction expression, final Object arg)
      throws ExpressionException
  {
    // @Override
    // public Disjunction toDNF() {
    //
    // // (F1 v ... v FN) = CONVERT(F1) v ... v CONVERT(FN)
    // final List<Disjunction> ds = new ArrayList<Disjunction>();
    // for (int i = 0; i < disjuncts.size(); i++) {
    // ds.add(disjuncts.get(i).toDNF());
    // }
    // final Disjunction d = ds.get(0);
    // for (int i = 1; i < ds.size(); i++) {
    // for (int j = 0; j < ds.get(i).size(); j++) {
    // d.append(ds.get(i).getMember(j));
    // }
    // }
    // return d;
    // }
    return false;
  }

  @Override
  public boolean visitField(final IFieldExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitFunctionCall(final IFunctionCallExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitLiteral(final ILiteral expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitMultiplication(final IMultiplication expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitNamed(final INamedExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitNegated(final INegatedExpression expression, final Object arg)
      throws ExpressionException
  {
    // @Override
    // public Disjunction toDNF() {
    //
    // // ~(~F) = CONVERT(F)
    // if (atom instanceof NegatedExpression) {
    // return ((NegatedExpression) atom).getExpression().toDNF();
    // }
    // // ~(F1 ^ ... ^ FN) = CONVERT(~F1 v ... v ~FN)
    // if (atom instanceof Conjunction) {
    // final Conjunction c = (Conjunction) atom;
    // final Disjunction d = new DisjunctionImpl(new NegatedAtomImpl(c.getMember(0)));
    // for (int i = 1; i < c.size(); i++) {
    // d.append(new NegatedAtomImpl(c.getMember(i)));
    // }
    // return d.toDNF();
    // }
    // // ~(F1 v ... v FN) = CONVERT(~F1 ^ ... ^ ~FN)
    // if (atom instanceof Disjunction) {
    // final Disjunction d = (Disjunction) atom;
    // final Conjunction c = new ConjunctionImpl(new NegatedAtomImpl(d.getMember(0)));
    // for (int i = 1; i < d.size(); i++) {
    // c.append(new NegatedAtomImpl(d.getMember(i)));
    // }
    // return c.toDNF();
    // }
    // // TODO: any other cases? not even relational expressions?
    // // in all other cases, the formula is in DNF
    // return new DisjunctionImpl(this);
    // }
    return false;
  }

  @Override
  public boolean visitNegative(final INegativeExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitRelational(final IRelationalExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }

  @Override
  public boolean visitSorted(final ISortedExpression expression, final Object arg)
      throws ExpressionException
  {
    // return new DisjunctionImpl(this);
    return false;
  }
}
