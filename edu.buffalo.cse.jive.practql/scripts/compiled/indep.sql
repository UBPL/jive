/*************************************************************
 *  REQUIRES: 
 * 
 *    1) running the ctdb.sql script                    
 * 
 *  PROVIDES: 
 * 
 *    1) implementation of the SQL/TP translation of all
 *       queries in Toman's SQL/TP paper
 *************************************************************
 */

DROP SCHEMA indep;
CREATE SCHEMA indep;
ALTER DEFAULT PRIVILEGES IN SCHEMA indep GRANT ALL ON TABLES TO public;
GRANT ALL ON SCHEMA indep TO public;

SET SEARCH_PATH TO indep,ctdb,public;

-- drop interval type
-- DROP TABLE indep CASCADE;

-- The following table and its data are from Jan's survey on temporal databases (1995)
CREATE TABLE Indep (
  country VARCHAR(100) NOT NULL, 
  capital VARCHAR(100) NOT NULL, 
  years cinterval
);

DELETE FROM Indep;

-- data is on page 37
-- one tuple modified from the original to generate a singleton interval in Q2
-- (the modified tuple is signaled in the comments)
INSERT INTO Indep(country, capital, years)
VALUES 
  ('Czech Kingdom',  'Prague',     (1198, 1621)),  -- {1198, ..., 1620}
  ('Czechoslovakia', 'Prague',     (1918, 1939)),  -- {1918, ..., 1938} 
  ('Czechoslovakia', 'Prague',     (1945, 1993)),  -- {1945, ..., 1992}
  ('Czech Republic', 'Prague',     (1993, NULL)),  -- {1993, ...}
  ('Slovakia',       'Bratislava', (1940, 1946)),  -- {1939*, ..., 1945}
  ('Slovakia',       'Bratislava', (1993, NULL)),  -- {1993, ...}
  ('Poland',         'Gniezo',     (1025, 1040)),  -- {1025, ..., 1039}
  ('Poland',         'Cracow',     (1040, 1596)),  -- {1040, ..., 1595}
  ('Poland',         'Warsaw',     (1596, 1795)),  -- {1596, ..., 1794} 
  ('Poland',         'Warsaw',     (1918, 1939)),  -- {1918, ..., 1938}
  ('Poland',         'Warsaw',     (1945, NULL));  -- {1945, ...}

-- Example 3.3 (p10-12) from Toman's SQL/TP paper

-- Q1 (abstract, SPJ)
--
-- SELECT r1.country
-- FROM indep r1, indep r2
-- WHERE r2.country = 'Czech Kingdom' and r1.year = r2.year

-- Q1 (concrete)
-- ~1ms
SELECT DISTINCT r1.country FROM Indep r1, Indep r2
WHERE r2.country  = 'Czech Kingdom' AND overlapping(r1.years, r2.years);

-- Q2 (abstract, complementation of 'true')
--
-- SELECT t AS year FROM true 
-- EXCEPT SELECT year FROM Indep

-- Q2 (concrete)
-- ~10ms
WITH 
  P1(intervals) AS (
    SELECT agg_partition(years) FROM (SELECT years FROM indep UNION SELECT t FROM _true) AS T
  )  
  SELECT project(t,intervals) FROM _true, P1
  EXCEPT SELECT project(years,intervals) FROM Indep, P1;


-- Q3 (abstract, complex double negation using 'true' for complementation)
-- 
-- PAPER:
--
-- SELECT y1, y2 FROM
--   (SELECT r1.t AS y1, r2.t AS y2 FROM true r1, true r2)        -- E3
--    EXCEPT 
--    SELECT y1, y2 FROM 
--    ((SELECT name, year AS y1, t AS y2 FROM Indep, true         -- E1
--      EXCEPT SELECT name, t AS y1, year AS y2 FROM Indep, true)
--    UNION
--     (SELECT name, t AS y1, year AS y2 FROM Indep, true         -- E2
--      EXCEPT SELECT name, year AS y1, t AS y2 FROM Indep, true)))
--
-- COMPILER INPUT (no support for nested queries):
--
-- WITH 
--   R1(name, y1, y2) AS (SELECT name, year, t FROM Indep, true),
--   E1(name, y1, y2) AS (SELECT name, y1, y2 FROM R1 EXCEPT SELECT name, y2, y1 FROM R1),
--   E2(name, y1, y2) AS (SELECT name, y2, y1 FROM R1 EXCEPT SELECT name, y1, y2 FROM R1),
--   U1(name, y1, y2) AS (SELECT name, y1, y2 FROM E1 UNION SELECT name, y1, y2 FROM E2),
--   TT(y1, y2) AS (SELECT T1.t, T2.t FROM true T1, true T2),
--   E3(y1, y2) AS (SELECT y1, y2 FROM TT EXCEPT SELECT y1, y2 FROM U1)
-- SELECT y1, y2 FROM E3;

-- Q3 (concrete)
-- ~30ms
WITH 
R(name, y1, y2) AS ( 
SELECT indep.country, indep.years, _true.t FROM indep, _true
), 
__R5(name, y1, y2) AS ( 
SELECT R.name, R.y1, R.y2 FROM R
), 
__R6(name, y2, y1) AS ( 
SELECT R.name, R.y2, R.y1 FROM R
), 
__U1(name, y1, y2) AS ( 
SELECT __R5.name, __R5.y1, __R5.y2 FROM __R5
UNION
SELECT __R6.name, __R6.y2, __R6.y1 FROM __R6
), 
__PAR5(name, Py1, Py2) AS ( 
SELECT __U1.name, AGG_PARTITION(__U1.y1) AS Py1, AGG_PARTITION(__U1.y2) AS Py2 FROM __U1 GROUP BY __U1.name
), 
__PRJ5(name, y1, y2) AS ( 
SELECT __R5.name, project(__R5.y1, __PAR5.Py1) AS y1, __R5.y2 FROM __R5, __PAR5 WHERE ((__R5.name = __PAR5.name))
), 
__PRJ6(name, y1, y2) AS ( 
SELECT __PRJ5.name, __PRJ5.y1, project(__PRJ5.y2, __PAR5.Py2) AS y2 FROM __PRJ5, __PAR5 WHERE ((__PRJ5.name = __PAR5.name))
), 
__PRJ7(name, y2, y1) AS ( 
SELECT __R6.name, project(__R6.y2, __PAR5.Py1) AS y2, __R6.y1 FROM __R6, __PAR5 WHERE ((__R6.name = __PAR5.name))
), 
__PRJ8(name, y2, y1) AS ( 
SELECT __PRJ7.name, __PRJ7.y2, project(__PRJ7.y1, __PAR5.Py2) AS y1 FROM __PRJ7, __PAR5 WHERE ((__PRJ7.name = __PAR5.name))
), 
E1(name, y1, y2) AS ( 
SELECT __PRJ6.name, __PRJ6.y1, __PRJ6.y2 FROM __PRJ6
EXCEPT
SELECT __PRJ8.name, __PRJ8.y2, __PRJ8.y1 FROM __PRJ8
), 
__R7(name, y2, y1) AS ( 
SELECT R.name, R.y2, R.y1 FROM R
), 
__R8(name, y1, y2) AS ( 
SELECT R.name, R.y1, R.y2 FROM R
), 
__U2(name, y2, y1) AS ( 
SELECT __R7.name, __R7.y2, __R7.y1 FROM __R7
UNION
SELECT __R8.name, __R8.y1, __R8.y2 FROM __R8
), 
__PAR6(name, Py2, Py1) AS ( 
SELECT __U2.name, AGG_PARTITION(__U2.y2) AS Py2, AGG_PARTITION(__U2.y1) AS Py1 FROM __U2 GROUP BY __U2.name
), 
__PRJ9(name, y2, y1) AS ( 
SELECT __R7.name, project(__R7.y2, __PAR6.Py2) AS y2, __R7.y1 FROM __R7, __PAR6 WHERE ((__R7.name = __PAR6.name))
), 
__PRJ10(name, y2, y1) AS ( 
SELECT __PRJ9.name, __PRJ9.y2, project(__PRJ9.y1, __PAR6.Py1) AS y1 FROM __PRJ9, __PAR6 WHERE ((__PRJ9.name = __PAR6.name))
), 
__PRJ11(name, y1, y2) AS ( 
SELECT __R8.name, project(__R8.y1, __PAR6.Py2) AS y1, __R8.y2 FROM __R8, __PAR6 WHERE ((__R8.name = __PAR6.name))
), 
__PRJ12(name, y1, y2) AS ( 
SELECT __PRJ11.name, __PRJ11.y1, project(__PRJ11.y2, __PAR6.Py1) AS y2 FROM __PRJ11, __PAR6 WHERE ((__PRJ11.name = __PAR6.name))
), 
E2(name, y1, y2) AS ( 
SELECT __PRJ10.name, __PRJ10.y2, __PRJ10.y1 FROM __PRJ10
EXCEPT
SELECT __PRJ12.name, __PRJ12.y1, __PRJ12.y2 FROM __PRJ12
), 
__R9(name, y1, y2) AS ( 
SELECT E1.name, E1.y1, E1.y2 FROM E1
), 
__R10(name, y1, y2) AS ( 
SELECT E2.name, E2.y1, E2.y2 FROM E2
), 
__U3(name, y1, y2) AS ( 
SELECT __R9.name, __R9.y1, __R9.y2 FROM __R9
UNION
SELECT __R10.name, __R10.y1, __R10.y2 FROM __R10
), 
__PAR7(name, Py1, Py2) AS ( 
SELECT __U3.name, AGG_PARTITION(__U3.y1) AS Py1, AGG_PARTITION(__U3.y2) AS Py2 FROM __U3 GROUP BY __U3.name
), 
__PRJ13(name, y1, y2) AS ( 
SELECT __R9.name, project(__R9.y1, __PAR7.Py1) AS y1, __R9.y2 FROM __R9, __PAR7 WHERE ((__R9.name = __PAR7.name))
), 
__PRJ14(name, y1, y2) AS ( 
SELECT __PRJ13.name, __PRJ13.y1, project(__PRJ13.y2, __PAR7.Py2) AS y2 FROM __PRJ13, __PAR7 WHERE ((__PRJ13.name = __PAR7.name))
), 
__PRJ15(name, y1, y2) AS ( 
SELECT __R10.name, project(__R10.y1, __PAR7.Py1) AS y1, __R10.y2 FROM __R10, __PAR7 WHERE ((__R10.name = __PAR7.name))
), 
__PRJ16(name, y1, y2) AS ( 
SELECT __PRJ15.name, __PRJ15.y1, project(__PRJ15.y2, __PAR7.Py2) AS y2 FROM __PRJ15, __PAR7 WHERE ((__PRJ15.name = __PAR7.name))
), 
U1(name, y1, y2) AS ( 
SELECT __PRJ14.name, __PRJ14.y1, __PRJ14.y2 FROM __PRJ14
UNION
SELECT __PRJ16.name, __PRJ16.y1, __PRJ16.y2 FROM __PRJ16
), 
TT(y1, y2) AS ( 
SELECT T1.t AS y1, T2.t AS y2 FROM _true AS T1, _true AS T2
), 
__R11(y1, y2) AS ( 
SELECT TT.y1, TT.y2 FROM TT
), 
__R12(y1, y2) AS ( 
SELECT U1.y1, U1.y2 FROM U1
), 
__U4(y1, y2) AS ( 
SELECT __R11.y1, __R11.y2 FROM __R11
UNION
SELECT __R12.y1, __R12.y2 FROM __R12
), 
__PAR8(Py1, Py2) AS ( 
SELECT AGG_PARTITION(__U4.y1) AS Py1, AGG_PARTITION(__U4.y2) AS Py2 FROM __U4
), 
__PRJ17(y1, y2) AS ( 
SELECT project(__R11.y1, __PAR8.Py1) AS y1, __R11.y2 FROM __R11, __PAR8
), 
__PRJ18(y1, y2) AS ( 
SELECT __PRJ17.y1, project(__PRJ17.y2, __PAR8.Py2) AS y2 FROM __PRJ17, __PAR8
), 
__PRJ19(y1, y2) AS ( 
SELECT project(__R12.y1, __PAR8.Py1) AS y1, __R12.y2 FROM __R12, __PAR8
), 
__PRJ20(y1, y2) AS ( 
SELECT __PRJ19.y1, project(__PRJ19.y2, __PAR8.Py2) AS y2 FROM __PRJ19, __PAR8
), 
E3(y1, y2) AS ( 
SELECT __PRJ18.y1, __PRJ18.y2 FROM __PRJ18
EXCEPT
SELECT __PRJ20.y1, __PRJ20.y2 FROM __PRJ20
)
SELECT E3.y1, E3.y2 FROM E3 ORDER BY (y1::cinterval)._left NULLS FIRST, (y2::cinterval)._left NULLS FIRST;

-- NOTE: the example below justifies projecting each dimension separately
--
-- SELECT unnest(ARRAY[1,2,3]) AS i, unnest(ARRAY[3,4]) AS j    --> WORKS
-- SELECT unnest(ARRAY[1,2,3]) AS i, unnest(ARRAY[4]) AS j      --> WORKS
-- SELECT unnest(ARRAY[1]) AS i, unnest(ARRAY[4]) AS j          --> WORKS
-- SELECT unnest(ARRAY[1,2,3]) AS i, unnest(ARRAY[3,4,5]) AS j  --> PROBLEM: arrays of the same length!
-- SELECT i, j FROM unnest(ARRAY[1,2,3]) AS A(i), unnest(ARRAY[3,4,5]) AS B(j)  -- WORKS, but array is in the FROM clause

-- Q4 (abstract, using aggregates)
-- 
-- PAPER:
--
-- SELECT DISTINCT country 
-- FROM indep, (SELECT MIN(year) as y0 FROM indep WHERE country = 'Slovakia') AS M
-- WHERE year < y0
--
-- COMPILER INPUT:
--
-- WITH 
--   M(y0) AS (
--     SELECT MIN(year) as y0 FROM indep WHERE country = 'Slovakia';
--   )
-- SELECT DISTINCT country FROM indep, M WHERE year < y0
--

-- Q4 (concrete)
-- ~10ms
WITH 
__R1(y0) AS ( 
SELECT AGG_MIN(indep.years) AS y0 FROM indep WHERE (indep.country = 'Slovakia')
)
SELECT DISTINCT indep.country FROM indep, __R1 AS M WHERE has_pred(indep.years, M.y0);

-- Q5
-- 
-- SELECT country, count(years) AS years
-- FROM indep
-- WHERE 1900 <= years AND years < 2000
-- GROUP BY country

-- Q5 (concrete)
-- ~10ms
SELECT indep.country, AGG_COUNT((GREATEST((indep.years::cinterval)._left, 1900), LEAST((indep.years::cinterval)._right, 2000))::cinterval) AS years 
FROM indep 
WHERE (has_succi(indep.years, 1900) AND has_pred(indep.years, 2000)) 
GROUP BY country;

-- Q6
--
-- SELECT year, count(country) AS numofc
-- FROM indep
-- GROUP BY year

-- Q6 (concrete)
-- ~10ms
WITH
  P1(y) AS (
    SELECT agg_partition(years) FROM indep
  ),
  N1(country, years) AS (
    SELECT country, project(years, y) FROM indep, P1
  )
SELECT years, COUNT(country) FROM N1 
GROUP BY years
ORDER BY get_left(years);

-- Additional Queries
--
-- AQ1:
--
-- SELECT DISTINCT year FROM indep
--
-- REWRITTEN AS:
--
-- SELECT year FROM indep GROUP BY year

-- AQ1 (concrete)
-- ~10ms
WITH
  P1(y) AS (
    SELECT agg_partition(years) FROM indep
  ),
  N1(years) AS (
    SELECT project(years, y) FROM indep, P1
  )
SELECT years FROM N1 GROUP BY years


WITH RECURSIVE 
  s(n) AS (
    VALUES (1)
  ),
  t(n) AS (
    select n from s
  UNION ALL
    SELECT n FROM u WHERE n < 100
  ),
  u(n) AS (
    SELECT t.n + 1 FROM t
  )
SELECT sum(n) FROM t;


WITH 
R(name, y1, y2) AS ( 
SELECT indep.country, indep.years, _true.t FROM indep, _true
), 
__R5(name, y1, y2) AS ( 
SELECT R.name, R.y1, R.y2 FROM R
), 
__R6(name, y2, y1) AS ( 
SELECT R.name, R.y2, R.y1 FROM R
), 
__U1(name, y1, y2) AS ( 
SELECT __R5.name, __R5.y1, __R5.y2 FROM __R5
UNION
SELECT __R6.name, __R6.y2, __R6.y1 FROM __R6
), 
__PAR5(name, Py1, Py2) AS ( 
SELECT __U1.name, AGG_PARTITION(__U1.y1) AS Py1, AGG_PARTITION(__U1.y2) AS Py2 FROM __U1 GROUP BY __U1.name
), 
__PRJ5(name, y1, y2) AS ( 
SELECT __R5.name, project(__R5.y1, __PAR5.Py1) AS y1, __R5.y2 FROM __R5, __PAR5 WHERE ((__R5.name = __PAR5.name))
), 
__PRJ6(name, y1, y2) AS ( 
SELECT __PRJ5.name, __PRJ5.y1, project(__PRJ5.y2, __PAR5.Py2) AS y2 FROM __PRJ5, __PAR5 WHERE ((__PRJ5.name = __PAR5.name))
), 
__PRJ7(name, y2, y1) AS ( 
SELECT __R6.name, project(__R6.y2, __PAR5.Py1) AS y2, __R6.y1 FROM __R6, __PAR5 WHERE ((__R6.name = __PAR5.name))
), 
__PRJ8(name, y2, y1) AS ( 
SELECT __PRJ7.name, __PRJ7.y2, project(__PRJ7.y1, __PAR5.Py2) AS y1 FROM __PRJ7, __PAR5 WHERE ((__PRJ7.name = __PAR5.name))
), 
E1(name, y1, y2) AS ( 
SELECT __PRJ6.name, __PRJ6.y1, __PRJ6.y2 FROM __PRJ6
EXCEPT
SELECT __PRJ8.name, __PRJ8.y2, __PRJ8.y1 FROM __PRJ8
), 
__R7(name, y2, y1) AS ( 
SELECT R.name, R.y2, R.y1 FROM R
), 
__R8(name, y1, y2) AS ( 
SELECT R.name, R.y1, R.y2 FROM R
), 
__U2(name, y2, y1) AS ( 
SELECT __R7.name, __R7.y2, __R7.y1 FROM __R7
UNION
SELECT __R8.name, __R8.y1, __R8.y2 FROM __R8
), 
__PAR6(name, Py2, Py1) AS ( 
SELECT __U2.name, AGG_PARTITION(__U2.y2) AS Py2, AGG_PARTITION(__U2.y1) AS Py1 FROM __U2 GROUP BY __U2.name
), 
__PRJ9(name, y2, y1) AS ( 
SELECT __R7.name, project(__R7.y2, __PAR6.Py2) AS y2, __R7.y1 FROM __R7, __PAR6 WHERE ((__R7.name = __PAR6.name))
), 
__PRJ10(name, y2, y1) AS ( 
SELECT __PRJ9.name, __PRJ9.y2, project(__PRJ9.y1, __PAR6.Py1) AS y1 FROM __PRJ9, __PAR6 WHERE ((__PRJ9.name = __PAR6.name))
), 
__PRJ11(name, y1, y2) AS ( 
SELECT __R8.name, project(__R8.y1, __PAR6.Py2) AS y1, __R8.y2 FROM __R8, __PAR6 WHERE ((__R8.name = __PAR6.name))
), 
__PRJ12(name, y1, y2) AS ( 
SELECT __PRJ11.name, __PRJ11.y1, project(__PRJ11.y2, __PAR6.Py1) AS y2 FROM __PRJ11, __PAR6 WHERE ((__PRJ11.name = __PAR6.name))
), 
E2(name, y1, y2) AS ( 
SELECT __PRJ10.name, __PRJ10.y2, __PRJ10.y1 FROM __PRJ10
EXCEPT
SELECT __PRJ12.name, __PRJ12.y1, __PRJ12.y2 FROM __PRJ12
), 
__R9(name, y1, y2) AS ( 
SELECT E1.name, E1.y1, E1.y2 FROM E1
), 
__R10(name, y1, y2) AS ( 
SELECT E2.name, E2.y1, E2.y2 FROM E2
), 
__U3(name, y1, y2) AS ( 
SELECT __R9.name, __R9.y1, __R9.y2 FROM __R9
UNION
SELECT __R10.name, __R10.y1, __R10.y2 FROM __R10
), 
__PAR7(name, Py1, Py2) AS ( 
SELECT __U3.name, AGG_PARTITION(__U3.y1) AS Py1, AGG_PARTITION(__U3.y2) AS Py2 FROM __U3 GROUP BY __U3.name
), 
__PRJ13(name, y1, y2) AS ( 
SELECT __R9.name, project(__R9.y1, __PAR7.Py1) AS y1, __R9.y2 FROM __R9, __PAR7 WHERE ((__R9.name = __PAR7.name))
), 
__PRJ14(name, y1, y2) AS ( 
SELECT __PRJ13.name, __PRJ13.y1, project(__PRJ13.y2, __PAR7.Py2) AS y2 FROM __PRJ13, __PAR7 WHERE ((__PRJ13.name = __PAR7.name))
), 
__PRJ15(name, y1, y2) AS ( 
SELECT __R10.name, project(__R10.y1, __PAR7.Py1) AS y1, __R10.y2 FROM __R10, __PAR7 WHERE ((__R10.name = __PAR7.name))
), 
__PRJ16(name, y1, y2) AS ( 
SELECT __PRJ15.name, __PRJ15.y1, project(__PRJ15.y2, __PAR7.Py2) AS y2 FROM __PRJ15, __PAR7 WHERE ((__PRJ15.name = __PAR7.name))
), 
U1(name, y1, y2) AS ( 
SELECT __PRJ14.name, __PRJ14.y1, __PRJ14.y2 FROM __PRJ14
UNION
SELECT __PRJ16.name, __PRJ16.y1, __PRJ16.y2 FROM __PRJ16
), 
TT(y1, y2) AS ( 
SELECT T1.t AS y1, T2.t AS y2 FROM _true AS T1, _true AS T2
), 
__R11(y1, y2) AS ( 
SELECT TT.y1, TT.y2 FROM TT
), 
__R12(y1, y2) AS ( 
SELECT U1.y1, U1.y2 FROM U1
), 
__U4(y1, y2) AS ( 
SELECT __R11.y1, __R11.y2 FROM __R11
UNION
SELECT __R12.y1, __R12.y2 FROM __R12
), 
__PAR8(Py1, Py2) AS ( 
SELECT AGG_PARTITION(__U4.y1) AS Py1, AGG_PARTITION(__U4.y2) AS Py2 FROM __U4
), 
__PRJ17(y1, y2) AS ( 
SELECT project(__R11.y1, __PAR8.Py1) AS y1, __R11.y2 FROM __R11, __PAR8
), 
__PRJ18(y1, y2) AS ( 
SELECT __PRJ17.y1, project(__PRJ17.y2, __PAR8.Py2) AS y2 FROM __PRJ17, __PAR8
), 
__PRJ19(y1, y2) AS ( 
SELECT project(__R12.y1, __PAR8.Py1) AS y1, __R12.y2 FROM __R12, __PAR8
), 
__PRJ20(y1, y2) AS ( 
SELECT __PRJ19.y1, project(__PRJ19.y2, __PAR8.Py2) AS y2 FROM __PRJ19, __PAR8
), 
E3(y1, y2) AS ( 
SELECT __PRJ18.y1, __PRJ18.y2 FROM __PRJ18
EXCEPT
SELECT __PRJ20.y1, __PRJ20.y2 FROM __PRJ20
)
SELECT E3.y1, E3.y2 FROM E3 ORDER BY (y1::cinterval)._left NULLS FIRST, (y2::cinterval)._left NULLS FIRST;

