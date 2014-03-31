package edu.buffalo.cse.jive.internal.practql.test;

import edu.buffalo.cse.jive.internal.practql.Factory;
import edu.buffalo.cse.jive.practql.schema.IRelationSchema;
import edu.buffalo.cse.jive.practql.schema.ISchemaFactory;
import edu.buffalo.cse.jive.practql.schema.SchemaException;
import edu.buffalo.cse.jive.practql.schema.Type;

/**
 * Comments
 * <ol>
 * <li>Supports only positive conjunctive queries with CTEs, grouping, and aggregation. No having
 * clause since they can be achieved by combining CTE and WHERE. Data types supported: BOOLEAN,
 * INTEGER, DECIMAL, STRING, TP, TP_ENCODED. Temporal expressions in projection, grouping, and
 * ordering are only supported as top-level field references (no complex expressions or
 * sub-expressions).</li>
 * </ol>
 * 
 * TODO
 * <ol>
 * <li>(syntax/semantics) validate group by (every non-aggregate field must be in the group by list
 * by name, to simplify validation; additional expressions may also appear in the group by).</li>
 * <li>(LATER: syntax/semantics) introduce 'IS INFINITY', 'IS NEGATIVE INFINITY' as relational
 * operators for TP types.</li>
 * </ol>
 */
class Test
{
  static final String[] DNFEXPs =
  { "A1 AND B1", "NOT(A1 AND B1)", "A1 OR (B1 AND (C1 OR D1))", "(A1 OR A2 AND A3) OR (A4 AND A5)",
      "(A1 AND A2 OR A3) AND (A4 OR A5)",
      "(A1 OR A2) AND (A3 OR A4) AND (A5 OR A6) AND (A7 OR A8) AND (A9 OR AA)", };
  static final String[] EXPs =
  {
      "R1.A || ' ' || R2.B = 'Foobar';",
      "TRUE AND F IS NOT NULL",
      "(1 < 2) AND (2 >= 1) AND (2 <> ABS(-3))",
      "1 AND (2 >= 1) + ABS(-3)",
      "(4 = GREATEST(5, 6) + LEAST(R.F * 2,7+-1,NULL))",
      "NOT (F = 7*3) OR 1 > -(SUM(G) - 10)",
      "(1 < 2) AND (2 >= 1) AND (2 <> ABS(-3)) OR (4 = GREATEST(5, 6) + LEAST(R.F * 2,7+-1,NULL)) OR (CEIL(4.5) <= 5) OR NOT (F = 7*3)",
      "true and false = true or true", "true and false = true or true = not true",
      "true and not false = true or false", "false or false = false and true" };
  // (OK) no repeated CTE name in a query definition
  // (OK) no repeated table name/tuple variable in the FROM list
  // (OK) no repeated field name in the projection list of a CTE
  // (OK) no repeated field name in the projection list of a query
  // (OK) no repeated field name in the projection list of a CTE after wildcard expansion
  // (OK) no repeated field name in the projection list of a query after wildcard expansion
  // (OK) field types are consistent across set/bag operations
  // (OK) field types are consistent even when defining a NULL field in an early query
  // (OK) no field type can remain NULL after validation
  // (NOT OK) group and aggregate validation
  static final String[] SQLs =
  {
      "SELECT R1.A, R1.A + 5, COUNT(DISTINCT 1*3 + 6+R1.A), COUNT(*), MAX(R2.B), MIN(R1.C), SUM(DISTINCT R1.A), 34, 45.32 FROM R1, R1 AS R2;",
      "SELECT R1.A, COUNT(DISTINCT 1*3 + 6+R1.A), COUNT(*), MAX(R2.B), MIN(R1.C), SUM(DISTINCT R1.A), 34, 45.32 FROM R1, R1 AS R2;",
      "WITH FOO(A, B, C, D, E) AS (SELECT *, D + C FROM R1), FOO2(A, B, C) AS (SELECT A, B, C FROM R1) SELECT Foo.A, Foo2.B, FOo2.C FROM FOO, Foo2;",
      "WITH FOO(A, B, C, D, E) AS (SELECT *, D + C FROM R1), FOO2(A, B, C) AS (SELECT A, B, C FROM R1) SELECT Foo.A, Foo2.A AS B, FOo2.B AS C FROM FOO, Foo2;",
      "SELECT R2.A FROM R1, R1 AS R2 WHERE R1.A > 5 AND 'Foo' || ' ' || 'Bar' = 'Foobar' AND R2.B < R1.A;",
      "SELECT R1.A, R2.B, 34, 45.32 FROM R1, R1 AS R2;",
      "SELECT R1.A AS A, R2.B AS B, 34, 45.32 FROM R1, R1 AS R2;",
      "SELECT * FROM R1, R1 AS R2 GROUP BY R1.A+R1.B*5;",
      "SELECT *, R2.B AS F1, 34, 45.32 FROM R1, R1 AS R2 WHERE 1 = 5 GROUP BY R2.B, F1;",
      "SELECT R2.A AS F1, R2.C  AS F2, 34 AS F3, 45.32  AS F4, 'String' AS Foo , 'String with ''quoted string'' inside' AS FooBar FROM R1, R1 AS R2 WHERE TRUE AND R1.A IS NOT NULL;",
      "SELECT R1.A, 'String with ''quoted string'' inside' AS Foo  FROM R1, R1 AS R2 WHERE (1 < 2) AND (2 >= 1) AND (2 <> 3) AND (4 = 5) AND (4 <= 5) AND (R1.A*R2.B = 7*3);",
      "SELECT R1.A, R1.B FROM R1 UNION SELECT R1.B, R1.A FROM R1 ;",
      "SELECT R1.A, NULL AS B FROM R1 UNION SELECT R1.B, R1.A FROM R1 ;",
      "SELECT R1.A, NULL AS B FROM R1 UNION SELECT R1.B, 'FooBar' AS C FROM R1 ;",
      "SELECT R1.A, NULL AS B FROM R1 UNION SELECT R1.B, 'FooBar' AS C FROM R1 UNION SELECT R1.B, 'C' FROM R1 ;",
      "SELECT R1.A, NULL AS B FROM R1 UNION SELECT R1.B, 5 FROM R1 UNION SELECT R1.B, 5.322 FROM R1;",
      "SELECT R1.B, 5 FROM R1 UNION SELECT R1.A, NULL AS B FROM R1 UNION SELECT R1.B, 5.322 FROM R1;",
      "SELECT R1.B, 5 FROM R1 UNION SELECT R1.B, 5.322 FROM R1 UNION SELECT R1.A, NULL AS B FROM R1;", };
  static final String[] NORMALIZATION =
  {
//      "SELECT DISTINCT env, (t + 1) AS foo FROM bindings;",
      "SELECT DISTINCT env, t FROM bindings;",
      "SELECT DISTINCT env, t FROM bindings WHERE 9500 = t;",
      "SELECT DISTINCT env, t FROM bindings WHERE 4500 <= t AND t < 10000;",
      "SELECT DISTINCT env, t FROM bindings WHERE env < 'env 28' AND 4500 <= t AND t < 10000;",
      "SELECT DISTINCT env, t FROM bindings WHERE env < 'env 28' AND 9000 <= t AND t < 10000;",
      "SELECT DISTINCT env, t FROM bindings WHERE env < 'env 22' AND 9500 <= t AND t < 10000;",
      "SELECT b1.env AS env1, b1.mbr AS mbr1, b2.env AS env2, b2.mbr AS mbr2, b1.val, b1.t FROM bindings AS b1, bindings AS b2 WHERE b1.env <> b2.env AND b1.val = b2.val AND b1.t = b2.t;",
      "SELECT DISTINCT env, MIN(t) AS agg FROM bindings GROUP BY env;",
      "SELECT DISTINCT env, MIN(t) AS agg FROM bindings GROUP BY env;",
      "SELECT DISTINCT env, MAX(t) AS agg FROM bindings GROUP BY env;",
      "SELECT DISTINCT env, COUNT(t) AS agg FROM bindings GROUP BY env;",
      "SELECT b1.env, b1.mbr, b1.val AS val1, b2.val AS val2, b1.t FROM bindings AS b1, bindings AS b2 WHERE b1.env = b2.env AND b1.mbr = b2.mbr AND b1.val < b2.val AND b1.t < b2.t;",
      "SELECT b1.env, b1.mbr, b1.val AS val1, b2.val AS val2, b1.t FROM bindings AS b1, bindings AS b2 WHERE b1.env = b2.env AND b1.mbr = b2.mbr AND b1.val > b2.val AND b1.t < b2.t;",
      "SELECT b1.env, b1.mbr, b1.val AS val1, b2.val AS val2, b1.t FROM bindings AS b1, bindings AS b2 WHERE b1.env = b2.env AND b1.mbr = b2.mbr AND b1.val < b2.val AND b1.t < b2.t EXCEPT SELECT b1.env, b1.mbr, b1.val AS val1, b2.val AS val2, b1.t FROM bindings AS b1, bindings AS b2 WHERE b1.env = b2.env AND b1.mbr = b2.mbr AND b1.val > b2.val AND b1.t < b2.t;", };
  
  static final String[] TEMPORAL =
  {
      //--------------------------------------------------------------------------------
      // SQL/TP Queries in PractQL
      //--------------------------------------------------------------------------------
      //
      // Q1 in SQL/TP
      "SELECT DISTINCT r1.country FROM Indep AS r1, Indep AS r2 WHERE r2.country = 'Czech Kingdom' and r1.years = r2.years;",
      //
      // Q2 in SQL/TP
      "SELECT t AS years FROM _true EXCEPT SELECT years FROM Indep;",
      //
      // Q3 in SQL/TP
      "WITH  " +
      "  M(y0) AS (SELECT MIN(years) as y0 FROM indep WHERE country = 'Slovakia') " +
      "SELECT DISTINCT country FROM indep, M WHERE years < y0;",
      //
      // Q4 in SQL/TP
      "SELECT country, COUNT(years) AS years " +
      "FROM indep " +
      "WHERE 1901 <= years AND years <= 2000 " +
      "GROUP BY country;",
      //
      // Q5 in SQL/TP
      "SELECT years, count(country) AS numofc " +
      "FROM indep " +
      "GROUP BY years;",
      //
      // Q6 in SQL/TP
      "WITH " +
      "  R(country, y1, y2) AS " +
      "    (SELECT country, years, t FROM Indep, _true), " +
      "  E1(country, y1, y2) AS " +
      "    (SELECT country, y1, y2 FROM R " +
      "     EXCEPT SELECT country, y2, y1 FROM R), " +
      "  E2(country, y1, y2) AS " +
      "    (SELECT country, y2, y1 FROM R " +
      "     EXCEPT SELECT country, y1, y2 FROM R), " +
      "  U1(country, y1, y2) AS " +
      "    (SELECT country, y1, y2 FROM E1 " +
      "     UNION SELECT country, y1, y2 FROM E2), " +
      "  TT(y1, y2) AS " +
      "    (SELECT T1.t AS y1, T2.t AS y2 " +
      "     FROM _true AS T1, _true AS T2), " +
      "  E3(y1, y2) AS " +
      "    (SELECT y1, y2 FROM TT " +
      "     EXCEPT SELECT y1, y2 FROM U1) " +
      "SELECT y1, y2 FROM E3;", 
      //--------------------------------------------------------------------------------
      "SELECT country, capital, years FROM Indep;",
      "SELECT * FROM Indep;",
      "SELECT * FROM Indep WHERE years > 2000;",
      "SELECT * FROM Indep WHERE years > 2000 AND years < 1900;",
      "SELECT * FROM Indep WHERE years = 10 AND years > 2000 AND years < 1900 AND years >= 1950 AND years <= 1975;",
      "SELECT DISTINCT years FROM Indep, R1 WHERE years > (12) AND years > years + 0 AND years + 0 > years;",
      "SELECT DISTINCT years FROM Indep WHERE 1900 <= years AND years < 2000;",
      "SELECT DISTINCT years FROM indep;",
      "SELECT years FROM Indep, R1 WHERE years > (12) AND years > years + 10 AND years + 10 > years;",
      "SELECT country, years FROM Indep WHERE 1900 <= years AND years < 2000;",
      "SELECT i1.country AS c1, i2.country AS c2, i1.years AS y1, i2.years AS y2 FROM Indep AS i1, Indep AS i2 WHERE i1.years = i2.years AND i1.country <> i2.country;",
      //
      //--------------------------------------------------------------------------------
      // ATSQL Queries in PractQL
      //--------------------------------------------------------------------------------
      //
      // Q1 in Querying ATSQL
      "WITH " +
      // Poland when it was independent
      "  P(years) AS (SELECT years FROM Indep WHERE country='Poland'),  " +
      // Slovakia when it was independent
      "  S(years) AS (SELECT years FROM Indep WHERE country='Slovakia') " +
      // Poland was independent but not Slovakia
      "  SELECT years FROM P EXCEPT SELECT years FROM S;", 
      //
      // Q2 in Querying ATSQL
      "WITH " +
      // Poland's capitals when it was independent
      "  P(capital, years) AS " +
      "    (SELECT capital, years " +
      "     FROM Indep " +
      "     WHERE country='Poland'),  " +
      // Poland was not independent
      "  NP(years) AS " +
      "    (SELECT t AS years " +
      "     FROM _true " +
      "     EXCEPT SELECT years FROM P),  " +
      // Poland when its capital was Cracow
      "  S1(capital, years) AS " +
      "    (SELECT capital, years " +
      "     FROM P " +
      "     WHERE capital = 'Cracow'), " +
      // Poland was either not independent or its capital was not Cracow
      "  S2(capital, years) AS " +
      "    (SELECT capital, years " +
      "     FROM P " +
      "     WHERE capital <> 'Cracow' " +
      "     UNION SELECT 'NOT INDEP' AS capital, years FROM NP), " +
      // all capitals that succeeded Cracow
      "  S3(capital, years) AS " +
      "    (SELECT R.capital, R.years " +
      "     FROM S1 AS L, S2 AS R " +
      "     WHERE L.years < R.years AND R.capital <> 'NOT INDEP'),  " +
      // all capitals that succeeded Cracow, but not directly-- 
      // either a not dependent state or an independent state 
      // with another capital exists in between
      "  S4(capital, years) AS " +
      "    (SELECT R.capital, R.years " +
      "     FROM S1 AS L, S2 AS M, S2 AS R " +
      "     WHERE L.years < M.years AND M.years < R.years AND M.capital <> R.capital)  " +
      // capitals that immediately succeeded Cracow (would work if Cracow had been capital more than once)
      "SELECT capital, years FROM S3 EXCEPT SELECT capital, years FROM S4;", 
      //
      // Q3 in Querying ATSQL
      "WITH " +
      // Countries
      "  C(country) AS (SELECT DISTINCT country FROM Indep),  " +
      // Countries when they were not independent
      "  NC(country, years) AS (SELECT C.country, T.t AS years FROM C, _true AS T EXCEPT SELECT country, years FROM Indep)  " +
      // Countries that lost and regained independence
      "SELECT DISTINCT NC.country, NC.years " +
      "FROM Indep AS L, NC, Indep AS R " +
      "WHERE L.country = NC.country AND NC.country = R.country AND " +
      "      L.years < NC.years AND NC.years < R.years;", 
      //
      // Q4 in Querying ATSQL
      "WITH " +
      // Countries
      "  C(country) AS " +
      "    (SELECT DISTINCT country FROM Indep),  " +
      // Countries when they were not independent
      "  NC(country, years) AS " +
      "    (SELECT C.country, T.t AS years " +
      "     FROM C, _true AS T " +
      "     EXCEPT SELECT country, years FROM Indep),  " +
      // Countries that were independent after not being independent
      // => we want a subset of the records returned by this relation, 
      //    namely all but those that changed capital while independent
      "  After(country, capital, years) AS " +
      "    (SELECT I.country, I.capital, I.years " +
      "     FROM NC, Indep AS I " +
      "     WHERE NC.country = I.country AND NC.years < I.years),  " +
      // Countries that changed capital: returns the later country/capital
      "  Changed(country, capital, years) AS " +
      "    (SELECT I.country, I.capital, I.years " +
      "     FROM Indep AS C, Indep AS I " +
      "     WHERE C.country = I.country AND C.capital <> I.capital AND C.years < I.years),  " +
      // Countries that changed capital while independent: returns the later country/capital
      "  IChanged(country, capital, years) AS " +
      "    (SELECT country, capital, years FROM Changed" +
      // Countries that changed capital and were not independent at some time in-between
      "     EXCEPT SELECT I.country, I.capital, I.years FROM Indep AS C, NC, Indep AS I " +
      "            WHERE C.country = I.country AND C.country = NC.country AND " +
      "                  C.capital = I.capital AND C.years < NC.years AND NC.years < I.years)  " +
      " SELECT country, capital, years FROM After " +
      " EXCEPT SELECT country, capital, years FROM IChanged;", 
      //--------------------------------------------------------------------------------
      //
      // Q4/v2 in Querying ATSQL
      "WITH " +
      // Countries when they were not independent
      "  NC(country, years) AS " +
      "    (SELECT DISTINCT country, T.t AS years FROM Indep, _true AS T " +
      "     EXCEPT SELECT country, years FROM Indep),  " +
      // Changed capital: returns the newer capital
      "  Changed(country, capital, years) AS " +
      "    (SELECT I.country, I.capital, I.years " +
      "     FROM Indep AS C, Indep AS I " +
      "     WHERE C.country = I.country AND C.capital <> I.capital AND C.years < I.years),  " +
      // Changed capital while independent: returns the newer capital
      "  IChanged(country, capital, years) AS " +
      "    (SELECT country, capital, years FROM Changed" +
      // Capitals with dependence gap
      "     EXCEPT SELECT I.country, I.capital, I.years FROM Indep AS C, NC, Indep AS I " +
      "            WHERE C.country = I.country AND C.country = NC.country AND " +
      "                  C.years < NC.years AND NC.years < I.years)  " +
      " SELECT country, capital, years FROM Indep " +
      " EXCEPT SELECT country, capital, years FROM IChanged;", 
      //--------------------------------------------------------------------------------
      //
      // Q4/v3 in Querying ATSQL
      "WITH " +
      // Countries when they were dependent
      "  Dep(country, years) AS " +
      "    (SELECT DISTINCT country, T.t AS years FROM Indep, _true AS T " +
      "     EXCEPT SELECT country, years FROM Indep),  " +
      // Not a first capital
      "  NotAdjacentToDep(country, capital, years) AS " +
      "    (SELECT A.country, A.capital, A.years " +
      "     FROM Indep AS M, Indep AS A " +
      "     WHERE M.country = A.country AND " +
      "           M.capital <> A.capital AND M.years < A.years  " +
      "     EXCEPT SELECT A.country, A.capital, A.years " +
      "     FROM Indep AS M, Dep AS MB, Indep AS A " +
      "     WHERE M.country = MB.country AND MB.country = A.country AND " +
      "           M.capital <> A.capital AND M.years < MB.years AND MB.years < A.years)  " +
      " SELECT country, capital, years FROM Indep " +
      " EXCEPT SELECT country, capital, years FROM NotAdjacentToDep;", 
      //--------------------------------------------------------------------------------
      //
      "SELECT DISTINCT name, x, y FROM Rect2D;"
      //--------------------------------------------------------------------------------
  };
  
  static final String[] TEMPORAL_RECURSIVE =
  { "WITH RECURSIVE REC(E1, E2, T) AS (SELECT E1, E2, T FROM graph UNION SELECT B.E1, R.E2, B.T FROM graph AS B, REC AS R WHERE B.E2 = R.E1 AND B.T = R.T) SELECT * FROM REC;", };

  static final String[] TEMPORAL_RELOPS =
  {
      "SELECT country, capital, years, ref FROM Indep WHERE years > 2000 ORDER BY years;",
      "SELECT country, capital, years, ref FROM Indep WHERE 2000 < years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years >= 2000;",
      "SELECT country, capital, years, ref FROM Indep WHERE 2000 <= years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years < 2000;",
      "SELECT country, capital, years, ref FROM Indep WHERE 2000 > years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years <= 2000;",
      "SELECT country, capital, years, ref FROM Indep WHERE 2000 >= years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years = 2000;",
      "SELECT country, capital, years, ref FROM Indep WHERE years = 2000 + years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years + 2000 = years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years = 2000 + ref;",
      "SELECT country, capital, years, ref FROM Indep WHERE years = 2000 - ref;", // TODO: DO NOT SUPPORT THIS!
      // "SELECT country, capital, years, ref FROM Indep WHERE years + ref = 2000;", // TODO: support this! 
      "SELECT country, capital, years, ref FROM Indep WHERE years > ref;",
      "SELECT country, capital, years, ref FROM Indep WHERE ref < years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years >= ref;",
      "SELECT country, capital, years, ref FROM Indep WHERE ref <= years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years < ref;",
      "SELECT country, capital, years, ref FROM Indep WHERE ref > years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years <= ref;",
      "SELECT country, capital, years, ref FROM Indep WHERE ref >= years;",
      "SELECT country, capital, years, ref FROM Indep WHERE years = ref;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years > I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years < I1.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years >= I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years <= I1.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years < I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years > I1.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years <= I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years >= I1.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years = I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + 10 > I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years < I1.years + 10;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + 10 >= I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years <= I1.years + 10;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + 10 < I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years > I1.years + 10;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + 10 <= I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years >= I1.years + 10;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + I2.ref > I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years < I1.years + I2.ref;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + I2.ref >= I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years <= I1.years + I2.ref;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + I2.ref < I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years > I1.years + I2.ref;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I1.years + I2.ref <= I2.years;",
      "SELECT I1.country, I1.capital, I1.years AS years1, I1.ref AS ref1, I2.years AS years2, I2.ref AS ref2 FROM Indep AS I1, Indep AS I2 WHERE I2.years >= I1.years + I2.ref;", };

  static final String[] TEMPORAL_RELOPS_2 =
  {
    "SELECT B1.env, B1.t FROM bindings AS B1, bindings AS B2, bindings AS B3 WHERE B1.env = B2.env AND B2.env = B3.env AND B1.t = B2.t AND B2.t = B3.t;",
    "SELECT B1.env, B1.t FROM bindings AS B1, bindings AS B2, bindings AS B3 WHERE B1.env = B2.env AND B2.env = B3.env AND B1.t = B2.t AND B2.t = B3.t AND B3.t = 100;",
  };
  
  static IRelationSchema getBindings() throws SchemaException
  {
    final ISchemaFactory factory = Factory.INSTANCE.schemaFactory();
    final IRelationSchema rs = factory.newRelationSchema("bindings",
        factory.newFieldSchema("env", Type.STRING));
    rs.getSignature().append(factory.newFieldSchema("mbr", Type.STRING));
    rs.getSignature().append(factory.newFieldSchema("val", Type.STRING));
    rs.getSignature().append(factory.newFieldSchema("t", Type.TP_ENCODED));
    return rs;
  }

  static IRelationSchema getGraph() throws SchemaException
  {
    final ISchemaFactory factory = Factory.INSTANCE.schemaFactory();
    final IRelationSchema rs = factory.newRelationSchema("graph",
        factory.newFieldSchema("E1", Type.INTEGER));
    rs.getSignature().append(factory.newFieldSchema("E2", Type.INTEGER));
    rs.getSignature().append(factory.newFieldSchema("T", Type.TP_ENCODED));
    return rs;
  }

  static IRelationSchema getIndep() throws SchemaException
  {
    final ISchemaFactory factory = Factory.INSTANCE.schemaFactory();
    final IRelationSchema rs = factory.newRelationSchema("indep",
        factory.newFieldSchema("country", Type.STRING));
    rs.getSignature().append(factory.newFieldSchema("capital", Type.STRING));
    rs.getSignature().append(factory.newFieldSchema("years", Type.TP_ENCODED));
    rs.getSignature().append(factory.newFieldSchema("ref", Type.TP));
    return rs;
  }

  static IRelationSchema getR1() throws SchemaException
  {
    final ISchemaFactory factory = Factory.INSTANCE.schemaFactory();
    final IRelationSchema rs = factory.newRelationSchema("r1",
        factory.newFieldSchema("A", Type.INTEGER));
    rs.getSignature().append(factory.newFieldSchema("B", Type.INTEGER));
    rs.getSignature().append(factory.newFieldSchema("C", Type.INTEGER));
    rs.getSignature().append(factory.newFieldSchema("D", Type.INTEGER));
    return rs;
  }

  static IRelationSchema getRect2D() throws SchemaException
  {
    final ISchemaFactory factory = Factory.INSTANCE.schemaFactory();
    final IRelationSchema rs = factory.newRelationSchema("Rect2D",
        factory.newFieldSchema("name", Type.STRING));
    rs.getSignature().append(factory.newFieldSchema("x", Type.TP_ENCODED));
    rs.getSignature().append(factory.newFieldSchema("y", Type.TP_ENCODED));
    return rs;
  }
  
  static IRelationSchema getTrue() throws SchemaException
  {
    final ISchemaFactory factory = Factory.INSTANCE.schemaFactory();
    final IRelationSchema rs = factory.newRelationSchema("_true",
        factory.newFieldSchema("t", Type.TP_ENCODED));
    return rs;
  }
}