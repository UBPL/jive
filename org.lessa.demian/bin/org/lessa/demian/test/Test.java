package org.lessa.demian.test;

class Test
{
  static final String[] EXPRESSIONS =
  { 
    "add(1,2)", 
    "add(1, mult(2,3))", 
    "mult(add(2, 2), div(9, 3))", 
    "let(a, 5, add(a, a))",
    "let(a, 5, let(b, mult(a, 10), add(b, a)))",
    "let(a, let(b, 10, add(b, b)), let(b, 20, add(a, b)))" 
  };
}