/*
Fast interval (of time or ip addresses) searching with spatial indexes

Searching intervals is usually slow, because the optimizer don't use an index. The reason lies in the dependency between the start and end columns. One solution is based on spatial indexes: it allows working with two dependent values as if they were a single value:

postgres=# EXPLAIN ANALYZE SELECT * FROM testip WHERE 19999999 BETWEEN startip AND endip;
                           QUERY PLAN
----------------------------------------------------------------
 Seq Scan on testip  (cost=0.00..19902.00 rows=200814 width=12) (actual time=3.457..434.218 rows=1 loops=1)
   Filter: ((19999999 >= startip) AND (19999999 <= endip))
 Total runtime: 434.299 ms
(3 rows)

Time: 435,865 ms

postgres=# CREATE INDEX ggg ON testip USING gist ((box(point(startip,startip),point(endip,endip))) box_ops);
CREATE INDEX
Time: 75530,079 ms
postgres=# EXPLAIN ANALYZE 
              SELECT * 
                 FROM testip 
                WHERE box(point(startip,startip),point(endip,endip)) @> box(point (19999999,19999999), point(19999999,19999999));
                                                                                                QUERY PLAN                                                            
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 Bitmap Heap Scan on testip  (cost=60.50..2550.14 rows=1000 width=12) (actual time=0.169..0.172 rows=1 loops=1)
   Recheck Cond: (box(point((startip)::double precision, (startip)::double precision), point((endip)::double precision, (endip)::double precision)) @> '(19999999,19999999),(19999999,19999999)'::box)
   ->  Bitmap Index Scan on ggg  (cost=0.00..60.25 rows=1000 width=0) (actual time=0.152..0.152 rows=1 loops=1)
         Index Cond: (box(point((startip)::double precision, (startip)::double precision), point((endip)::double precision, (endip)::double precision)) @> '(19999999,19999999),(19999999,19999999)'::box)
 Total runtime: 0.285 ms
(5 rows)

Time: 2,805 ms
*/

/* tables for normalization tests */
DROP TABLE Bindings CASCADE;
DROP TABLE CallTrees CASCADE;

create table Bindings (
  env varchar(10) not null, -- containing environment
  mbr varchar(10) not null, -- member
  val varchar(10) not null, -- value
  l int not null,
  r int null,
  constraint Bindings_interval check (l <= r)
);

create table CallTrees (
  thd int not null, -- thread of the call tree
  act varchar(20) not null, -- activation's call identifier
  mth varchar(10) not null, -- method name
  ctx varchar(10) not null, -- caller environment, e.g., class or instance
  l int not null,
  r int null,
  constraint CallTrees_interval check (l <= r)
);

-- around 180K + 36K + 2.4K
-- delete from calltrees
select * from create_random_calltree(1, 30, 20, 90, 10, 2000, 10, 10)
select * from create_random_calltree(2, 10, 15, 70, 20, 4000, 8, 12)
select * from create_random_calltree(3, 15, 10, 80, 15, 8000, 6, 16)

/*
  Query #1: Interval containment to compute ancestor-descendant relationships in call trees
*/
-- cost is 8K--582M
SELECT I.act, I.l, I.r, J.act, J.l, J.r 
FROM calltrees I inner join calltrees J on (I.thd = J.thd AND I.l <= J.l AND J.r <= I.r);

-- cost is 0--3M (up to 200x faster)
SELECT I.act, I.l, I.r, J.act, J.l, J.r 
FROM calltrees I 
INNER JOIN calltrees J ON (I.thd = J.thd AND box(point(I.L,I.L),point(I.R,I.R)) @> box(point(J.L,J.L), point(J.R,J.R)));

/*
  Query #2: Retrieval of specified subtree
*/
-- cost is 0--5146
SELECT *
FROM calltrees I WHERE (5000 <= I.l AND I.r <= 100000);

-- cost is 14--645 (up to 8x faster)
SELECT *
FROM calltrees I WHERE (box(point(5000,5000),point(100000,100000)) @> box(point(I.L,I.L),point(I.R,I.R)));

select thd, min(L), max(R), count(*) from calltrees group by thd;

CREATE INDEX calltrees_interval ON calltrees USING gist ((box(point(l,l),point(r,r))) box_ops);

-- VACUUM ANALYZE calltrees;
-- select thd, count(*) from calltrees group by thd;
-- select * from calltrees where thd=3 order by l limit 100

-- select * from create_random_bindings(10000, 15, 5, 5000, 5000);
-- VACUUM ANALYZE bindings;
-- select count(*) from bindings;
-- select * from bindings order by env, r desc, mbr limit 1000;

select count(*) from callTrees;
select * from callTrees limit 100;
vacuum analyze callTrees;
vacuum analyze bindings;
select * from callTrees T1, callTrees T2 
where T1.thd=1 AND T2.thd=2 and T1.l <= T2.r and T2.l <= T1.r;
drop view Source1

create index ix_bindings_env on bindings(env);
create index ix_bindings_left on bindings(l);
create index ix_bindings_right on bindings(r);
create index ix_bindings_left_right on bindings(l,r);

CREATE VIEW Source1 (env, l, r) AS
  SELECT env, l, r FROM bindings;

  vacuum analyze bindings

-- left interval endpoints associated with each datum of the source relations
CREATE VIEW L(env, mbr, val, l) AS

  -- 771K -- 10K rows 51sec

  WITH 
  left_pts(env, T) AS (
    SELECT env, l FROM bindings UNION SELECT env, r + 1 FROM bindings),
  right_pts(env, T) AS (
    SELECT env, r FROM bindings UNION SELECT env, l - 1 FROM bindings),
  NotImin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN left_pts I ON L.env = I.env AND L.T < I.T 
    INNER JOIN right_pts R ON L.env = R.env AND I.T <= R.T
    UNION SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts I ON L.env = I.env AND L.T <= I.T 
    INNER JOIN right_pts R ON L.env = R.env AND I.T < R.T),
  Imin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.env = R.env AND L.T <= R.T
    EXCEPT SELECT env, L, R FROM NotImin) 

  -- push selections manually, namely, make sure we only look at intervals that intersect (5000, 50000)
  WITH 
  left_pts(env, T) AS (
    SELECT env, l FROM bindings 
    WHERE (L > 5000 AND L < 50000) OR (R > 5000 AND R < 50000) OR (L < 5000 AND R > 50000)
    UNION SELECT env, r + 1 FROM bindings
    WHERE (L > 5000 AND L < 50000) OR (R > 5000 AND R < 50000) OR (L < 5000 AND R > 50000)),
  right_pts(env, T) AS (
    SELECT env, r FROM bindings 
    WHERE (L > 5000 AND L < 50000) OR (R > 5000 AND R < 50000) OR (L < 5000 AND R > 50000)
    UNION SELECT env, l - 1 FROM bindings
    WHERE (L > 5000 AND L < 50000) OR (R > 5000 AND R < 50000) OR (L < 5000 AND R > 50000)),
  NotImin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN left_pts I ON L.env = I.env AND L.T < I.T 
    INNER JOIN right_pts R ON L.env = R.env AND I.T <= R.T
    UNION SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts I ON L.env = I.env AND L.T <= I.T 
    INNER JOIN right_pts R ON L.env = R.env AND I.T < R.T),
  Imin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.env = R.env AND L.T <= R.T
    EXCEPT SELECT env, L, R FROM NotImin) 

  -- does not push selections!
  select * from Imin where L > 5000 and R < 50000

select * from calltrees where l <= 1000 limit 100;

select c1.act from calltrees c1
where c1.l < 1000 AND
      exists (select c2.act from calltrees c2 where c2.r <= c1.r group by c2.act having sum(c2.l) < c1.r);

SELECT DISTINCT c1.act from calltrees c1 INNER JOIN calltrees c2 ON c2.r <= c1.r 
WHERE c1.l < 1000 GROUP BY c1.act, c2.act, c1.r HAVING sum(c2.l) < c1.r;


  WITH 
  interval_pts(env, Ls, Rs) AS (
    -- 70K
CNT
-- general solution to normalized bag/set operations
--   1) factor out subqueries as CTEs
--   2) create a union of subqueries on X, t1, ..., tn
--   3) compute the minimal intervals for each X in the union
--   4) compute the normalized version of the CTEs in (1)
--   5) perform the set/bag operation involving the CTEs in (4)
     WITH 
       CTE1(Y1, ..., YM1, X1, ..., XN, TS, TE) AS ( ... ),
       ...
       CTEK(Y1, ..., YMk, X1, ..., XN, TS, TE) AS ( ... ),
       Union1(X1, ..., XN, TS, TE) AS (
         SELECT X1, ..., XN, TS, TE 
         FROM (SELECT X1, ..., XN, TS, TE FROM CTE1
               UNION [ALL] SELECT X1, ..., XN, TS, TE FROM CTE2
               ...
               UNION [ALL] SELECT X1, ..., XN, TS, TE FROM CTEK) AS U),
       IMin(X1, ..., XN, TS, TE) AS (
         SELECT DISTINCT X1, ..., XN, pt[0] AS i_L, pt[1] AS i_R FROM
           (SELECT
              X1, ..., XN, 
              build_intervals(array_sort('<left endpoint literals>'::int[] || array_agg(l) || array_agg(r+1)), 
                              array_sort('<right endpoint literals>'::int[] || array_agg(r) || array_agg(l-1))) AS pt
            FROM Union1)),
       NormalCTE1(Y1, ..., YM1, X1, ..., XN, TS, TE) AS (
         SELECT Y1, ..., YM1, X1, ..., XN, I.i_L, I.i_R FROM CTE1 C NATURAL JOIN IMin I)
       ...
       NormalCTEK(Y1, ..., YMk, X1, ..., XN, TS, TE) AS (
         SELECT Y1, ..., YMk, X1, ..., XN, I.i_L, I.i_R FROM CTEK C NATURAL JOIN IMin I),
       NormalUnion1(X1, ..., XN, TS, TE) AS (
         SELECT X1, ..., XN, TS, TE 
         FROM (SELECT X1, ..., XN, TS, TE FROM NormalCTE1
               UNION [ALL] SELECT X1, ..., XN, TS, TE FROM NormalCTE2
               ...
               UNION [ALL] SELECT X1, ..., XN, TS, TE FROM NormalCTEK) AS U)
               

    SELECT DISTINCT pt[0] AS L, pt[1] AS R FROM
    (SELECT
       build_intervals(array_sort('{5001,9001}'::int[] || array_agg(l) || array_agg(r+1)), 
                       array_sort('{6999,9999}'::int[] || array_agg(r) || array_agg(l-1))) AS pt
     FROM bindings WHERE ((L > 5000 AND R < 7000) OR (L > 9000 AND R < 10000))) AS F ORDER BY 2

    -- proof of concept for normalization!
    -- the literals referenced in the query must be part of the left/right endpoints
    SELECT env, unnest(pts) AS pt FROM
    (SELECT
       env, 
       agg_count(build_intervals2(array_sort('{5001,9001}'::int[] || array_agg(l) || array_agg(r+1)), 
                       array_sort('{6999,9999}'::int[] || array_agg(r) || array_agg(l-1)))) AS pts
     FROM bindings WHERE ((L > 5000 AND R < 7000) OR (L > 9000 AND R < 10000)) GROUP BY env) AS F 
select count(*) from bindings
    SELECT DISTINCT L FROM bindings WHERE ((L > 5000 AND R < 7000) OR (L > 9000 AND R < 10000)) ORDER BY 1

select distinct L[0], L[1], R[0], R[1] from (values (point '(0,0)', point '(1,1)'), (point '(0,0)', point '(1,1)') ) AS F(L,R)

select (1, 2)::c_interval
select * FROM (values ((1, 2)::c_interval), ((1, 2)::c_interval)) AS R
select DISTINCT * FROM (values ((1, 2)::c_interval), ((1, 2)::c_interval), ((1, 3)::c_interval)) AS R
WITH v1(a) AS (
select ARRAY(select * FROM (values ((1, 2)::c_interval), ((1, 2)::c_interval), ((1, 3)::c_interval)) AS R)),
v2(a) AS (
select ARRAY(select * FROM (values ((1, 2)::c_interval), ((1, 3)::c_interval), ((1, 3)::c_interval)) AS R))
select distinct array_set_union(v1.a, v2.a) from v1 natural full outer join v2
select * from v1 union select * from v2

CREATE OR REPLACE FUNCTION array_set_intersect(ANYARRAY, ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $$
SELECT ARRAY(
  SELECT $1[i] FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
  INTERSECT SELECT $2[j] FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j); $$;

CREATE OR REPLACE FUNCTION array_bag_intersect(ANYARRAY, ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $$
SELECT ARRAY(
  SELECT $1[i] FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
  INTERSECT ALL SELECT $2[j] FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j); $$;

CREATE OR REPLACE FUNCTION array_set_difference(ANYARRAY, ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $$
SELECT ARRAY(
  SELECT $1[i] FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
  EXCEPT SELECT $2[j] FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j); $$;

CREATE OR REPLACE FUNCTION array_bag_difference(ANYARRAY, ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $$
SELECT ARRAY(
  SELECT $1[i] FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
  EXCEPT ALL SELECT $2[j] FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j); $$;

CREATE OR REPLACE FUNCTION array_set_union(ANYARRAY, ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $$
SELECT ARRAY(
  SELECT $1[i] FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
  UNION SELECT $2[j] FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j); $$;

CREATE OR REPLACE FUNCTION array_bag_union(ANYARRAY, ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $$
SELECT ARRAY(
  SELECT $1[i] FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
  UNION ALL SELECT $2[j] FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j); $$;

select * from (values (1, 1), (0,1)) AS F

 SELECT
    env, agg_count(pts), agg_min(pts), agg_max(pts)
 FROM
    (SELECT
       env, 
       build_intervals2(array_sort('{2001}'::int[] || array_agg(l) || array_agg(r+1)), 
                        array_sort('{6999}'::int[] || array_agg(r) || array_agg(l-1))) AS pts
     FROM bindings WHERE L > 2000 AND R < 7000 GROUP BY env) AS F
 WHERE (agg_max(pts) - agg_min(pts)) - agg_count(pts) <> -1

SELECT env, 
       build_intervals(array_sort('{2001,4001}'::int[] || array_agg(l) || array_agg(r+1)), 
                       array_sort('{4000,6999}'::int[] || array_agg(r) || array_agg(l-1))) AS pts
     FROM bindings WHERE L > 2000 AND R < 7000 GROUP BY env
UNION SELECT env, 
       build_intervals(array_sort('{2001,4001}'::int[] || array_agg(l) || array_agg(r+1)), 
                       array_sort('{7000,8999}'::int[] || array_agg(r) || array_agg(l-1))) AS pts
     FROM bindings WHERE L > 4000 AND R < 9000 GROUP BY env

 
  IMin(env, intervals) AS (
    SELECT B.env, make_intervals(B.L, B.R, Ls, Rs)
    FROM bindings B INNER JOIN interval_pts ip ON (B.env = ip.env))
  select * from Imin WHERE L > 5000 and R < 50000
  select '{5001}'::int[] 
  Imin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.env = R.env AND L.T <= R.T
    EXCEPT SELECT env, L, R FROM NotImin) 


  create or replace view v_left_pts(env, T) AS 
    SELECT env, l FROM bindings UNION SELECT env, r + 1 FROM bindings;
    
  create or replace view v_right_pts(env, T) AS 
    SELECT env, r FROM bindings UNION SELECT env, l - 1 FROM bindings;
    
  create or replace view v_NotImin(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM v_left_pts L 
    INNER JOIN v_left_pts I ON L.env = I.env AND L.T < I.T 
    INNER JOIN v_right_pts R ON L.env = R.env AND I.T <= R.T
    UNION SELECT L.env, L.T, R.T 
    FROM v_left_pts L 
    INNER JOIN v_right_pts I ON L.env = I.env AND L.T <= I.T 
    INNER JOIN v_right_pts R ON L.env = R.env AND I.T < R.T;
    
  create or replace view v_Imin(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM v_left_pts L 
    INNER JOIN v_right_pts R ON L.env = R.env AND L.T <= R.T
    EXCEPT SELECT env, L, R FROM v_NotImin;

  -- does not push selections!
  select * from v_Imin where L > 5000 and R < 50000
select count(*) from bindings 

select * from bindings limit 1000
  -- 852K -- 1.1M rows 
  SELECT env, mbr, val, l FROM bindings
  UNION SELECT env, mbr, val, r + 1 FROM bindings
  ORDER BY 2;

  -- 180K/59K -- 10K rows in 21/2.9 secs (indexing on env helps!)
  SELECT env, array_sort(array_agg(l) || array_agg(r+1)) AS Rs FROM bindings
  GROUP BY env;
  
  -- 59K -- 10K rows in 2.9secs
  SELECT env, array_sort(AL || AR) FROM 
    (SELECT env, array_agg(l) AS AL, array_agg(r+1) AS AR FROM bindings
     GROUP BY env) AS F;

  -- 228K -- 1.1M rows in 57secs
  SELECT env, mbr, val, array_sort(array_agg(l) || array_agg(r+1)) AS Rs FROM bindings
  GROUP BY env, mbr, val;
  
CREATE OR REPLACE FUNCTION array_sort(ANYARRAY) RETURNS ANYARRAY
LANGUAGE SQL AS $$
  SELECT ARRAY(
    SELECT DISTINCT
      $1[s.i] AS "foo"
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    ORDER BY foo
); $$;

CREATE FUNCTION int_array_sort(ANYARRAY) RETURNS ANYARRAY
LANGUAGE SQL AS $$
  SELECT ARRAY(
    SELECT DISTINCT
      $1[s.i] AS "foo"
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    WHERE $1[s.i] IS NOT NULL
    ORDER BY foo
); $$;

CREATE OR REPLACE FUNCTION make_intervals_array(IN L INT, IN R INT, IN Ls INT[], IN Rs INT[]) RETURNS point[]
LANGUAGE SQL AS $$
  SELECT ARRAY(SELECT * FROM make_intervals($1,$2,$3,$4));
$$;

CREATE OR REPLACE FUNCTION build_intervals3(IN Ls INT[], IN Rs INT[]) RETURNS point[] LANGUAGE SQL AS $$
  SELECT ARRAY( SELECT * FROM build_intervals($1, $2) ); $$;

-- build interval array
CREATE OR REPLACE FUNCTION build_intervals2(IN Ls INT[], IN Rs INT[]) RETURNS point[] LANGUAGE SQL AS $$
  SELECT ARRAY( SELECT * FROM build_intervals($1, $2) ); $$;

select * 
from unnest('{point(1,0),point(2,0),point(3,0),point(4,0),point(5,0),point(6,0),point(7,0),point(8,0),point(9,0)}'::point[]) AS pt

select * from unnest('{(1,0),(2,0),(3,0),(4,0),(5,0),(6,0),(7,0),(8,0),(9,0)}'::point[]) AS pt

CREATE OR REPLACE FUNCTION agg_min(IN pts point[]) RETURNS double precision LANGUAGE SQL AS
$BODY$ SELECT MIN(pt[0]) FROM unnest($1) AS pt; $BODY$ 

CREATE OR REPLACE FUNCTION agg_max(IN pts point[]) RETURNS double precision LANGUAGE SQL AS
$BODY$ SELECT MAX(pt[1]) FROM unnest($1) AS pt; $BODY$ 

CREATE OR REPLACE FUNCTION agg_count(IN pts point[]) RETURNS double precision LANGUAGE SQL AS
$BODY$ SELECT SUM(pt[1] - pt[0] + 1) FROM unnest($1) AS pt; $BODY$ 

-- build interval set
CREATE OR REPLACE FUNCTION build_intervals(IN Ls INT[], IN Rs INT[], OUT pt point) RETURNS SETOF point AS
$BODY$
DECLARE
  i INT;
  j INT;
  max_i INT;
  max_j INT;
  flag BOOLEAN;
BEGIN
  i := array_lower(Ls,1);
  j := array_lower(Rs,1);
  max_i := array_upper(Ls,1);
  max_j := array_upper(Rs,1);

  -- find and return the first interval, if any
  IF (i <= max_i AND j <= max_j) THEN
    -- move to first right endpoint greater or equal to the current left endpoint
    flag = true;
    WHILE (j <= max_j AND flag) LOOP
      IF (Ls[i] > Rs[j]) THEN
        j := j + 1;
      ELSE
        flag = false;
      END IF;
    END LOOP;
    -- there may be no intervals to build
    IF (j > max_j) THEN
      RETURN;
    END IF;
    -- move to last left endpoint smaller than current right endpoint
    WHILE (i < max_i AND flag) LOOP
      IF (Ls[i+1] <= Rs[j]) THEN
        i := i + 1;
      ELSE
        flag = false;
      END IF;
    END LOOP;
    pt := point(Ls[i], Rs[j]);
    RETURN NEXT;
  END IF;
  i := i + 1;
  j := j + 1;
  -- interval left and right endpoints are now aligned
  WHILE (i <= max_i AND j <= max_j) LOOP
    pt := point(Ls[i], Rs[j]);
    RETURN NEXT;
    i := i + 1;
    j := j + 1;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION make_intervals(IN L INT, IN R INT, IN Ls INT[], IN Rs INT[]) RETURNS point[]
LANGUAGE SQL AS $$
  SELECT ARRAY(
    SELECT point($3[L.i], $4[R.i])
--      $1[s.i] AS "foo"
    FROM
      generate_series(array_lower($3,1), array_upper($3,1)) AS L(i),
      generate_series(array_lower($4,1), array_upper($4,1)) AS R(i)
    WHERE
      $3[L.i] <= $4[r.i]);
$$;

select * from make_intervals(0,0,'{0,10,21}','{1,11,45}')
select * from build_intervals('{0,10,21}','{1,11,45}')

select ARRAY(select 1,2)

-- right interval endpoints associated with each datum of the source relations
CREATE OR REPLACE VIEW R(datum, r) AS
  SELECT datum, r FROM Source1
  UNION SELECT datum, l - 1 FROM Source1
  UNION SELECT datum, r FROM Source2
  UNION SELECT datum, l - 1 FROM Source2;

-- non-minimal intervals associated with each datum of the source relations
CREATE OR REPLACE VIEW NImin(datum, l, r) AS
  SELECT L1.datum, L1.l, R.r
  FROM L AS L1 
  INNER JOIN L AS L2 ON (L1.datum = L2.datum AND L1.l < L2.l)
  INNER JOIN R ON (L1.datum = R.datum AND L2.l <= r);

-- minimal intervals associated with each datum of the source relations
CREATE OR REPLACE VIEW Imin(datum, l, r) AS
  SELECT L.datum, L.l, R.r 
  FROM L INNER JOIN R ON (L.datum = R.datum AND L.l <= R.r)
  LEFT JOIN NImin N ON (L.datum = N.datum AND L.l = N.l AND R.r = N.r)
  WHERE N.datum IS NULL;

-- Source1 relation with intervals partitioned into minimal intervals
CREATE OR REPLACE VIEW N_Source1(datum, l, r) AS
  SELECT S.datum, I.l, I.r
  FROM Source1 AS S
  INNER JOIN Imin I ON (S.datum = I.datum AND S.l <= I.l AND I.r <= S.r);
  
-- Source2 relation with intervals partitioned into minimal intervals
CREATE OR REPLACE VIEW N_Source2(datum, l, r) AS
  SELECT S.datum, I.l, I.r
  FROM Source2 AS S
  INNER JOIN Imin I ON (S.datum = I.datum AND S.l <= I.l AND I.r <= S.r);


DROP FUNCTION random_count(IN tableName text);
CREATE OR REPLACE FUNCTION random_count(IN tableName text) RETURNS SETOF INT AS
$BODY$
DECLARE
  r RECORD;
BEGIN
  FOR r IN EXECUTE 'SELECT COUNT(*) AS Val FROM ' || quote_ident(tableName) LOOP
    RETURN NEXT r.Val;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

select * from random_count('bindings');


-- drop interval type
DROP TYPE c_interval CASCADE;

-- create interval type
--   constraints cannot be defined here
CREATE TYPE c_interval AS (i_left int, i_right int);

-- select a single interval
SELECT (1,2)::c_interval AS I;

-- select the left interval endpoint
SELECT ((1,2)::c_interval).i_left;

-- select the right interval endpoint
SELECT ((1,2)::c_interval).i_right;

-- select a constant bag of intervals
SELECT I, (I::c_interval).i_left, (I::c_interval).i_right
FROM (VALUES ((1,10)::c_interval), ((5,15)::c_interval), ((5,15)::c_interval), ((15,25)::c_interval)) AS F(I);

-- select a constant set of intervals
SELECT DISTINCT I, (I::c_interval).i_left, (I::c_interval).i_right
FROM (VALUES ((1,10)::c_interval), ((5,15)::c_interval), ((5,15)::c_interval), ((15,25)::c_interval)) AS F(I);

-- returns the smallest instant contained in the interval array
CREATE OR REPLACE FUNCTION c_interval_agg_min(IN intervals c_interval[]) RETURNS INT LANGUAGE SQL AS
$BODY$ SELECT MIN(i_left) FROM unnest($1) AS F(i_left, i_right); $BODY$ 

-- returns the largest instant contained in the interval array
CREATE OR REPLACE FUNCTION c_interval_agg_max(IN intervals c_interval[]) RETURNS INT LANGUAGE SQL AS
$BODY$ SELECT MAX(i_right) - 1 FROM unnest($1) AS F(i_left, i_right); $BODY$ 

-- returns the duration of the intervals in the array
CREATE OR REPLACE FUNCTION c_interval_agg_count(IN intervals c_interval[]) RETURNS BIGINT LANGUAGE SQL AS
$BODY$ SELECT SUM(i_right - i_left) FROM unnest($1) AS F(i_left, i_right); $BODY$ 

-- builds a normal interval set
-- assumes Ls and Rs are sorted in ascending ordered
CREATE OR REPLACE FUNCTION c_interval_normal_set(IN Ls INT[], IN Rs INT[], OUT _i c_interval) RETURNS SETOF c_interval AS
$BODY$
DECLARE
  i INT;
  j INT;
  max_i INT;
  max_j INT;
  flag BOOLEAN;
BEGIN
  i := array_lower(Ls,1);
  j := array_lower(Rs,1);
  max_i := array_upper(Ls,1);
  max_j := array_upper(Rs,1);
  -- find and return the first interval, if any
  IF (i <= max_i AND j <= max_j) THEN
    -- move to first right endpoint greater or equal to the current left endpoint
    flag = true;
    WHILE (j <= max_j AND flag) LOOP
      IF (Ls[i] > Rs[j]) THEN
        j := j + 1;
      ELSE
        flag = false;
      END IF;
    END LOOP;
    -- there may be no intervals to build
    IF (j > max_j) THEN
      RETURN;
    END IF;
    -- move to last left endpoint smaller than current right endpoint
    WHILE (i < max_i AND flag) LOOP
      IF (Ls[i+1] <= Rs[j]) THEN
        i := i + 1;
      ELSE
        flag = false;
      END IF;
    END LOOP;
    _i := (Ls[i], Rs[j]);
    RETURN NEXT;
  END IF;
  i := i + 1;
  j := j + 1;
  -- interval left and right endpoints are now aligned
  WHILE (i <= max_i AND j <= max_j) LOOP
    _i := (Ls[i], Rs[j]);
    RETURN NEXT;
    i := i + 1;
    j := j + 1;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- decomposes the input interval into a set of normalized intervals based on the set of input intervals
CREATE OR REPLACE FUNCTION c_interval_normalized_set(IN L INT, IN R INT, IN _iin c_interval[], OUT _iout c_interval) RETURNS SETOF c_interval LANGUAGE SQL ROWS 10 AS 
$BODY$ 
  SELECT GREATEST($1, i_left), LEAST($2, i_right) 
  FROM unnest($3) AS F(i_left, i_right)
  WHERE $1 <= i_right AND i_left <= $2;
$BODY$ 

CREATE OR REPLACE FUNCTION c_interval_normalized_set(IN L INT, IN R INT, IN _iin c_interval[], OUT _iout c_interval) RETURNS SETOF c_interval AS
$BODY$
DECLARE
  ep_left INT;
  ptr INT;
  max_ptr INT;
  flag BOOLEAN;
BEGIN
  flag := true;
  ep_left := L;
  ptr := array_lower(_iin,1);
  max_ptr := array_upper(_iin,1);
  WHILE ptr <= max_ptr AND flag LOOP
    IF c_interval_contains_left(ep_left, _iin[ptr]) THEN
      _iout.i_left := ep_left;
      IF c_interval_contains_right(R, _iin[ptr]) THEN
        _iout.i_right := R;
        RETURN NEXT;
        flag := false;
      ELSE
        _iout.i_right := _iin[ptr].i_right;
        RETURN NEXT;
        ep_left := _iout.i_right + 1;
      END IF;
    END IF;
    ptr := ptr + 1;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql; 

CREATE OR REPLACE FUNCTION c_interval_normalized_set(IN L INT, IN R INT, IN _eps INT[], OUT _iout c_interval) RETURNS SETOF c_interval AS
$BODY$
DECLARE
  ptr INT;
  val_L INT;
  val_R INT;
  max_ptr INT;
  min_ptr INT;
BEGIN
  min_ptr := array_lower(_eps,1);
  max_ptr := array_upper(_eps,1);
  val_L := COALESCE(L,_eps[min_ptr]);
  val_R := COALESCE(R,_eps[max_ptr]);
  FOR ptr IN min_ptr..max_ptr-1 LOOP
    IF (val_L <= _eps[ptr]) AND (_eps[ptr+1] <= val_R) THEN
      _iout.i_left := _eps[ptr];
      _iout.i_right := _eps[ptr+1];
      RETURN NEXT;
    END IF;
  END LOOP;
  IF (L IS NULL) THEN
    _iout.i_left := NULL;
    _iout.i_right := val_L;
    RETURN NEXT;
  END IF;
  IF (R IS NULL) THEN
    _iout.i_left := val_R;
    _iout.i_right := NULL;
    RETURN NEXT;
  END IF;
  RETURN;
END; $BODY$ LANGUAGE plpgsql; 

-- build interval array
-- use case: in normalization, allows the representation of one temporal dimension as a set/bag of intervals (non-1NF)
--           this overcomes the explosion in the number of tuples in the normalized relations; however, the size of a
--           normalized interval set may still be O(n^2) after normalization; normalizing K fields will not yield an
--           exponential blow-up, though, only increasing the size of the encoded tuple to O(K*n^2) = O(n^2). Hence,
--           this approach provides a *real* benefit to the encoding and processing of temporal relations; of course,
--           this is only useful as part of the black-box approach to compiled queries; users must not be otherwise
--           exposed to this
--
--   UNION [ALL].......: (a) for each subquery, GROUP BY data attributes and aggregate by building interval bags
--                       (b) UNION subqueries with array_set/bag_union on attributes encoded as interval bags
--
--   INTERSECT [ALL]...: (a) NATURAL INNER JOIN on data attributes
--                       (b) array_set/bag_intersect on attributes encoded as interval bags
--                       (c) filter out tuples with empty interval bags
--
--   EXCEPT [ALL]......: (a) for each subquery, GROUP BY data attributes and aggregate by building interval bags 
--                       (b) EXCEPT subqueries with array_set/bag_difference on attributes encoded as interval bags
--                       (c) filter out tuples with empty interval bags
--
--   DISTINCT..........: (a) GROUP BY data attributes and aggregate by building interval bags
--                       (b) remove duplicates from interval bags
--
--   GROUP BY..........: grouping on a temporal requires that temporal to be 
--                       (a) create a CTE with GROUP BY on grouped attributes and attributes used in aggregates (G+A)
--                           and aggregate by building interval bags
--                       (b) rewrite the query against the CTE by grouping and aggregating just as in the original query;
--                           
--
CREATE OR REPLACE FUNCTION c_interval_normal_array(IN Ls INT[], IN Rs INT[]) RETURNS c_interval[] LANGUAGE SQL AS $$
  SELECT ARRAY( SELECT (i_left, i_right)::c_interval FROM c_interval_normal_set($1, $2) AS F(i_left, i_right) ); $$;

CREATE OR REPLACE FUNCTION c_interval_normalized_array(IN L INT, IN R INT, IN _eps INT[]) RETURNS c_interval[] LANGUAGE SQL AS $$
  SELECT ARRAY( SELECT (i_left, i_right)::c_interval FROM c_interval_normalized_set($1, $2, $3) AS F(i_left, i_right) ); $$;

CREATE OR REPLACE FUNCTION c_interval_normalized_array(IN L INT, IN R INT, IN _iin c_interval[]) RETURNS c_interval[] LANGUAGE SQL AS $$
  SELECT ARRAY( SELECT (i_left, i_right)::c_interval FROM c_interval_normalized_set($1, $2, $3) AS F(i_left, i_right) ); $$;

CREATE OR REPLACE FUNCTION c_interval_array_sort_left(ANYARRAY) RETURNS ANYARRAY
LANGUAGE SQL AS $$
  SELECT ARRAY(
    SELECT DISTINCT
      $1[s.i] AS "foo"
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    ORDER BY foo NULLS FIRST
); $$;

CREATE OR REPLACE FUNCTION c_interval_array_sort_right(ANYARRAY) RETURNS ANYARRAY
LANGUAGE SQL AS $$
  SELECT ARRAY(
    SELECT DISTINCT
      $1[s.i] AS "foo"
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    ORDER BY foo NULLS LAST
); $$;

-- MOVED to test1a
-- normalization implemented using a first order query w/o subqueries
-- selections pushed onto the helper predicates to reduce scalability issues 
WITH 
  left_pts(mbr, T) AS (
    SELECT DISTINCT mbr, 2001 FROM bindings 
    UNION SELECT mbr, l FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
    UNION SELECT mbr, r + 1 FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000),
  right_pts(mbr, T) AS (
    SELECT DISTINCT mbr, 6999 FROM bindings 
    UNION SELECT mbr, r FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
    UNION SELECT mbr, l - 1 FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000),
  NotImin(mbr, L, R) AS (
    SELECT L.mbr, L.T, R.T 
    FROM left_pts L 
    INNER JOIN left_pts I ON L.mbr = I.mbr AND L.T < I.T 
    INNER JOIN right_pts R ON L.mbr = R.mbr AND I.T <= R.T
    UNION SELECT L.mbr, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts I ON L.mbr = I.mbr AND L.T <= I.T 
    INNER JOIN right_pts R ON L.mbr = R.mbr AND I.T < R.T),
  Imin(mbr, L, R) AS (
    SELECT L.mbr, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.mbr = R.mbr AND L.T <= R.T
    EXCEPT SELECT mbr, L, R FROM NotImin) 
 SELECT
    mbr, l, r, GREATEST(BNS.L, l) as i_left, LEAST(BNS.R, r) as i_right
 FROM
    bindings B NATURAL JOIN IMin BNS 
 WHERE B.L <= BNS.R AND BNS.L <= R AND 
       mbr = 'mbm 11' AND L > 2000 AND R < 7000;

-- MOVED to test1a
-- normalization implemented using a first order query w/o subqueries
-- selections pushed onto the helper predicates to reduce scalability issues 
WITH 
  left_pts(mbr, T) AS (
    SELECT DISTINCT mbr, 2001 FROM bindings 
    UNION SELECT mbr, l FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
    UNION SELECT mbr, r + 1 FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000),
  right_pts(mbr, T) AS (
    SELECT DISTINCT mbr, 6999 FROM bindings 
    UNION SELECT mbr, r FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
    UNION SELECT mbr, l - 1 FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000),
  Imin(mbr, L, R) AS (
    SELECT L.mbr, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.mbr = R.mbr AND L.T <= R.T
    WHERE 
      NOT EXISTS(SELECT * FROM left_pts I WHERE L.mbr = I.mbr AND L.T < I.T AND I.T <= R.T)
      AND NOT EXISTS(SELECT * FROM right_pts I WHERE L.mbr = I.mbr AND L.T <= I.T AND I.T < R.T))
 SELECT
    B.mbr, B.l, B.r, GREATEST(BNS.L, B.l) as i_left, LEAST(BNS.R, B.r) as i_right
 FROM
    bindings B INNER JOIN IMin BNS ON B.mbr = BNS.mbr
 WHERE B.L <= BNS.R AND BNS.L <= B.R AND 
       B.mbr = 'mbm 11' AND B.L > 2000 AND B.R < 7000;

-- MOVED to test1a
-- normalization implemented using a first order query w/ subqueries
-- selections pushed onto the helper predicates to reduce scalability issues 
WITH 
  left_pts(mbr, T) AS (
    SELECT mbr, unnest(array_sort('{2001}'::int[] || array_agg(l) || array_agg(r+1)))
    FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
    GROUP BY mbr),
  right_pts(mbr, T) AS (
    SELECT mbr, unnest(array_sort('{6999}'::int[] || array_agg(r) || array_agg(l-1)))
    FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
    GROUP BY mbr),
  Imin(mbr, L, R) AS (
    SELECT L.mbr, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.mbr = R.mbr AND L.T <= R.T
    WHERE 
      NOT EXISTS(SELECT * FROM left_pts I WHERE L.mbr = I.mbr AND L.T < I.T AND I.T <= R.T)
      AND NOT EXISTS(SELECT * FROM right_pts I WHERE L.mbr = I.mbr AND L.T <= I.T AND I.T < R.T))
 SELECT
    B.mbr, B.l, B.r, GREATEST(BNS.L, B.l) as i_left, LEAST(BNS.R, B.r) as i_right
 FROM
    bindings B INNER JOIN IMin BNS ON B.mbr = BNS.mbr
 WHERE B.L <= BNS.R AND BNS.L <= B.R AND 
       B.mbr = 'mbm 11' AND B.L > 2000 AND B.R < 7000;

-- normalization implemented using a first order query w/ subqueries
-- selections pushed onto the helper predicates to reduce scalability issues 
WITH 
  endpoints(mbr, Ls, Rs) AS (
    SELECT 
      mbr,
      array_sort(ARRAY[2001] || int_array_agg(ARRAY[L, R+1])) AS Ls, 
      array_sort(ARRAY[6999] || int_array_agg(ARRAY[R, L-1])) AS Rs
   FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000 GROUP BY mbr),
  Imin(mbr, intervals) AS (
   SELECT mbr, c_interval_normal_array(Ls, Rs) FROM endpoints),
  query(mbr, L, R, intervals) AS (
    SELECT mbr, L, R, c_interval_normalized_array(L, R, intervals)
    FROM bindings B NATURAL JOIN IMin
    WHERE B.L > 2000 AND B.R < 7000)
  SELECT mbr, L, R, intervals FROM query;

-- normalization implemented using array aggregates and the normalized relation unfolded
-- 5.6M tuples in 427sec if unfolded onto minimal intervals (worst-case would be 17M tuples, so this is pretty close!)
WITH binding_normal_set(mbr, pts) AS (
   SELECT
      mbr,
      c_interval_normal_set(array_sort('{2001}'::int[] || array_agg(l) || array_agg(r+1)), 
                            array_sort('{6999}'::int[] || array_agg(r) || array_agg(l-1))) AS pts
   FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000 GROUP BY mbr
)
 SELECT
    mbr, l, r, GREATEST((pts::c_interval).i_left, l) as i_left, LEAST((pts::c_interval).i_right, r) as i_right
 FROM
    bindings B NATURAL JOIN binding_normal_set BNS 
 WHERE B.L <= (BNS.pts::c_interval).i_right AND (BNS.pts::c_interval).i_left <= R AND 
       mbr = 'mbm 11' AND L > 2000 AND R < 7000;

-- normalization implemented using array aggregates and the normalized relation associated with sets/bags of intervals
-- 5.8K tuples in 17secs if minimal intervals encoded as sets with their tuples
WITH binding_normal_set(mbr, pts) AS (
   SELECT
      mbr,
      c_interval_normal_array(array_sort(ARRAY[2001] || int_array_agg(ARRAY[L, R+1])), 
                              array_sort(ARRAY[6999] || int_array_agg(ARRAY[R, L-1]))) AS pts
   FROM bindings WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000 GROUP BY mbr
)
 SELECT
    mbr, l, r, c_interval_normalized_array(L,R,pts)
 FROM
    bindings B NATURAL JOIN binding_normal_set
 WHERE L > 2000 AND R < 7000;

CREATE FUNCTION int_array_append(IN a1 int[], IN a2 int[]) RETURNS int[] LANGUAGE SQL AS $BODY$ 
  SELECT $1 || $2;
$BODY$

CREATE AGGREGATE int_array_agg(int[]) (
  SFUNC=int_array_append,
  STYPE=int[],
  INITCOND='{}'
);

CREATE FUNCTION c_interval_wappend(IN agg c_interval, IN item INT) RETURNS c_interval LANGUAGE SQL AS $BODY$ 
  SELECT CASE WHEN $1 IS NULL THEN ($2,NULL)::c_interval ELSE ($1.i_left,$2)::c_interval END;
$BODY$

CREATE AGGREGATE c_interval_wagg(int) (
  SFUNC=c_interval_wappend,
  STYPE=c_interval,
  INITCOND='(,)'
);

select c_interval_wagg(v) from (VALUES (1), (2), (3)) AS F(v)

CREATE AGGREGATE int_array_agg(int[]) (
  SFUNC=int_array_append,
  STYPE=int[],
  INITCOND='{}'
);

CREATE FUNCTION c_interval_array_append(IN a1 c_interval[], IN a2 c_interval[]) RETURNS c_interval[] LANGUAGE SQL AS $BODY$ 
  SELECT $1 || $2;
$BODY$

CREATE AGGREGATE c_interval_array_agg(c_interval[]) (
  SFUNC=c_interval_array_append,
  STYPE=c_interval[],
  INITCOND='{}'
);

SELECT ARRAY[1001, 1002] || ARRAY[1002];

-- although the plan cost is low, the actual execution time is quite high (2+ minutes)
SELECT 
  mbr,
  c_interval_normalized_array(L,R,
     c_interval_normal_array(array_sort(ARRAY[2001] || int_array_agg(ARRAY[L, R+1]) OVER w), 
                             array_sort(ARRAY[6999] || int_array_agg(ARRAY[R, L-1]) OVER w)))
FROM bindings 
WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
WINDOW w AS (PARTITION BY mbr);

select ARRAY[2000], ARRAY[l] from bindings limit 1

select * from bindings b left outer join bindings b2 on b.env=b2.mbr where b2.mbr is null


--
-- The following table and its data are from Jan's survey on temporal databases (1995)
--
CREATE TABLE Indep (
  country VARCHAR(100) NOT NULL, 
  capital VARCHAR(100) NOT NULL, 
  years c_interval,
  CONSTRAINT pk_indep PRIMARY KEY(country, capital, years),
  CONSTRAINT ck_interval CHECK((years::c_interval).i_left <= (years::c_interval).i_right)
);
DELETE FROM Indep;
INSERT INTO Indep(country, capital, years)
VALUES ('Czech Kingdom', 'Prague', (1198, 1620)),
       ('Czechoslovakia', 'Prague', (1918, 1938)),
       ('Czechoslovakia', 'Prague', (1945, 1992)),
       ('Czech Republic', 'Prague', (1995, NULL)),
       ('Slovakia', 'Bratislava', (1940, 1944)),
       ('Slovakia', 'Bratislava', (1993, NULL)),
       ('Poland', 'Gniezo', (1025, 1039)),
       ('Poland', 'Cracow', (1040, 1595)),
       ('Poland', 'Warsaw', (1596, 1794)),
       ('Poland', 'Warsaw', (1918, 1938)),
       ('Poland', 'Warsaw', (1945, NULL));

-- Example 3.3 from Toman's SQL/TP paper
-- Q1
-- SELECT r1.country
-- FROM indep r1, indep r2
-- WHERE r2.country = 'Czech Kingdom' and r1.year = r2.year

-- ~1ms
SELECT DISTINCT r1.country FROM Indep r1, Indep r2
WHERE r2.country  = 'Czech Kingdom' AND c_interval_overlaps(r1.years, r2.years);

-- Q2
-- SELECT t AS year FROM true 
-- EXCEPT SELECT year FROM Indep

-- ~10ms
WITH 
  normal_indep(intervals) AS (
   SELECT
      c_interval_normal_array(c_interval_array_sort_left(int_array_agg(ARRAY[(years::c_interval).i_left, (years::c_interval).i_right+1])), 
                              c_interval_array_sort_right(int_array_agg(ARRAY[(years::c_interval).i_right, (years::c_interval).i_left-1]))) AS pts
   FROM (SELECT years FROM indep UNION SELECT (i_left, i_right)::c_interval FROM c_interval_true() AS T(i_left, i_right)) AS T
  ),
  t_set(intervals) AS (
   SELECT c_interval_normalized_set(i_left,i_right,intervals) FROM c_interval_true(), normal_indep
  ),
  indep_set(intervals) AS (
   SELECT c_interval_normalized_set((years::c_interval).i_left,(years::c_interval).i_right,intervals) FROM Indep, normal_indep
  )
  select * from t_set except select * from indep_set; 

-- Q3
-- SELECT y1, y2 FROM
-- (SELECT r1.t AS y1, r2.t AS y2 FROM true r1, true r2)  
--  EXCEPT SELECT y1, y2 FROM 
--  ( (SELECT name, year AS y1, t AS y2 FROM Indep, true
--     EXCEPT SELECT name, t AS y1, year AS y2 FROM Indep, true)
--    UNION
--    (SELECT name, t AS y1, year AS y2 FROM Indep, true
--     EXCEPT SELECT name, year AS y1, t AS y2 FROM Indep, true) ) )
--
--
-- set/bag queries must have the form SELECT <list> FROM CTE1 set/bagop SELECT <list> FROM CTE2 ...
-- (WHERE conditions can be applied at a different level in the CTE query chain)
--
-- distinct queries must have the form SELECT DISTINCT <list> FROM CTE
-- (WHERE conditions can be applied at a different in the CTE query chain)
--
-- group/aggregate queries must have the form SELECT <group_list>, <agg_list> FROM CTE GROUP BY <group_list>
-- (HAVING conditions can be applied at a higher level in the CTE query chain)
--
-- WITH 
--   Ryt_a(name, y1, y2) AS (SELECT name, year, t FROM Indep, true),
--   Ryt_b(name, y1, y2) AS (SELECT name, t, year FROM Indep, true),
--   E1(name, y1, y2) AS (SELECT name, y1, y2 FROM Ryt_a EXCEPT SELECT name, y1, y2 FROM Ryt_b),
--   E2(name, y1, y2) AS (SELECT name, y1, y2 FROM Ryt_b EXCEPT SELECT name, y1, y2 FROM Ryt_a),
--   U1(name, y1, y2) AS (SELECT name, y1, y2 FROM E1 UNION SELECT name, y1, y2 FROM E2),
--   TT(y1, y2) AS (SELECT T1.t, T2.t FROM true T1, true T2),
--   E3(y1, y2) AS (SELECT y1, y2 FROM TT EXCEPT SELECT y1, y2 FROM U1)
-- SELECT y1, y2 FROM E3;
--
-- 1) normalize(Q):: 
--    normalize(E3):: 
--    normalize(TT, U1) ^ normalize(U1, TT):: 
--    normalize(E1, E2) ^ normalize(E1, E2)::
--    normalize(Ryt_a, Ryt_b) ^ normalize(Ryt_b, Ryt_a)
-- 2) normalize(Ryt_a, Ryt_b) ^ normalize(Ryt_b, Ryt_a) 
--    a) compute normalizing_sets for Ryt_a and Ryt_b
--    b) compute normalized_Ryt_a and normalized_Ryt_b
--    c) create normalized_E1 as E1 but with normalized_Ryt_a and normalized_Ryt_b in place of Ryt_a and Ryt_b
--    d) create normalized_E2 as E2 but with normalized_Ryt_a and normalized_Ryt_b in place of Ryt_a and Ryt_b
-- 3) normalize(E1, E2) ^ normalize(E1, E2)
--    a) compute normalizing_sets for E1 and E2
--    b) compute normalized_E1 and normalized_E2
--    c) create normalized_U1 as U1 but with normalized_E1 and normalized_E2 in place of E1 and E2
-- 4) normalize(TT, U1) ^ normalize(U1, TT)
--    a) compute normalizing_sets for TT and U1
--    b) compute normalized_TT and normalized_U1
--    c) create normalized_E3 as E3 but with normalized_TT and normalized_U1 in place of TT and U1
-- 5) normalize(Q)
--    a) replace E3 in Q with normalized_E3
-- 
-- computes all normalizing sets for data attribute X and temporal attributes t1, ..., tk for R1, ..., Rn
-- equivalent to:
--   the Mi's are the relations to normalize next
--   for i = 1..n 
--     Mi = Ri
--   end
--   normalize on each temporal attribute
--   for j = 1..k
--     normalize all relations
--     for i = 1..n
--       Ni = N{X,tj}(Ri;{M1, ..., Mn})
--     end
--     for i = 1..n
--       Mi = Ni
--     end
--   end
--
-- using SQL and array aggregates the above is accomplished in two steps-- 
--   the first step computes normalizing sets, that is, the set of minimal intervals for each temporal attribute
--   the second pass computes normalized relations using a natural joining with the normalizing_sets relation
--
-- normalizing_sets(X, t1, ..., tk, R1, ..., Rn)
--   SELECT X, agg_normal_interval_set(t1) AS ns1, ..., agg_normal_interval_set(tk) AS nsk
--   FROM (SELECT X, t1, ..., tk FROM R1 UNION ... UNION SELECT X, t1, ..., tk FROM Rn)
--   GROUP BY X
--
-- normalized_R1
--   SELECT X, Y, normalized_intervals(t1, ns1), ..., normalized_intervals(tk, nsk)
--   FROM R1 NATURAL JOIN normalizing_sets
--
-- normalized_Rn
--   SELECT X, Y, normalized_intervals(t1, ns1), ..., normalized_intervals(tk, nsk)
--   FROM Rn NATURAL JOIN normalizing_sets
--


-- ~20ms
WITH
  Ryt(country, y1, y2) AS (
    SELECT country, years, (i_left, i_right)::c_interval
    FROM indep, c_interval_true() AS T(i_left, i_right)
  ),
  normalarrays_1(country, y1, y2) AS (
   SELECT
      country, 
      c_interval_normal_array(c_interval_array_sort_left(int_array_agg(ARRAY[(y1::c_interval).i_left, (y1::c_interval).i_right+1])), 
                              c_interval_array_sort_right(int_array_agg(ARRAY[(y1::c_interval).i_right, (y1::c_interval).i_left-1]))),
      c_interval_normal_array(c_interval_array_sort_left(int_array_agg(ARRAY[(y2::c_interval).i_left, (y2::c_interval).i_right+1])), 
                              c_interval_array_sort_right(int_array_agg(ARRAY[(y2::c_interval).i_right, (y2::c_interval).i_left-1])))
   FROM (SELECT country, y1, y2 FROM Ryt UNION SELECT country, y2, y1 FROM Ryt) AS SQ
   GROUP BY country),
  normalized_Ryt_folded(country, y1, y2) AS (
   SELECT 
     Ryt.country,
     c_interval_normalized_array((Ryt.y1::c_interval).i_left,(Ryt.y1::c_interval).i_right,NA.y1), 
     c_interval_normalized_array((Ryt.y2::c_interval).i_left,(Ryt.y2::c_interval).i_right,NA.y2)
   FROM Ryt INNER JOIN normalarrays_1 NA ON Ryt.country = NA.country
  ),
  normalized_Ryt_y1(country, y1, y2) AS (
   SELECT country, unnest(y1), y2 FROM normalized_Ryt_folded
  ),
  normalized_Ryt_y1_y2(country, y1, y2) AS (
   SELECT country, y1, unnest(y2) FROM normalized_Ryt_y1
  ),
  E1(country, y1, y2) AS (
   SELECT country, y1, y2 FROM normalized_Ryt_y1_y2 EXCEPT SELECT country, y2, y1 FROM normalized_Ryt_y1_y2
  ),
  E2(country, y1, y2) AS (
   SELECT country, y2, y1 FROM normalized_Ryt_y1_y2 EXCEPT SELECT country, y1, y2 FROM normalized_Ryt_y1_y2
  ),
  U1(country, y1, y2) AS (
   SELECT country, y1, y2 FROM E1 UNION SELECT country, y1, y2 FROM E2
  ),
  TT(y1, y2) AS (
   SELECT (T1.i_left, T1.i_right)::c_interval, (T2.i_left, T2.i_right)::c_interval
   FROM c_interval_true() AS T1(i_left, i_right), c_interval_true() AS T2(i_left, i_right)
  ),
  normalarrays_2(y1, y2) AS (
   SELECT
      c_interval_normal_array(c_interval_array_sort_left(int_array_agg(ARRAY[(y1::c_interval).i_left, (y1::c_interval).i_right+1])), 
                              c_interval_array_sort_right(int_array_agg(ARRAY[(y1::c_interval).i_right, (y1::c_interval).i_left-1]))),
      c_interval_normal_array(c_interval_array_sort_left(int_array_agg(ARRAY[(y2::c_interval).i_left, (y2::c_interval).i_right+1])), 
                              c_interval_array_sort_right(int_array_agg(ARRAY[(y2::c_interval).i_right, (y2::c_interval).i_left-1])))
   FROM (SELECT y1, y2 FROM U1 UNION SELECT y1, y2 FROM TT) AS SQ),
  normalized_tt_folded(y1, y2) AS (
   SELECT 
     c_interval_normalized_array((TT.y1::c_interval).i_left,(TT.y1::c_interval).i_right,na.y1), 
     c_interval_normalized_array((TT.y2::c_interval).i_left,(TT.y2::c_interval).i_right,na.y2)
   FROM TT, normalarrays_2 na
  ),
  normalized_tt_y1(y1, y2) AS (
   SELECT unnest(y1), y2 FROM normalized_tt_folded
  ),
  normalized_tt_y1_y2(y1, y2) AS (
   SELECT y1, unnest(y2) FROM normalized_tt_y1
  ),
  normalized_u1_folded(y1, y2) AS (
   SELECT 
     c_interval_normalized_array((U1.y1::c_interval).i_left,(U1.y1::c_interval).i_right,na.y1), 
     c_interval_normalized_array((U1.y2::c_interval).i_left,(U1.y2::c_interval).i_right,na.y2)
   FROM U1, normalarrays_2 na
  ),
  normalized_u1_y1(y1, y2) AS (
   SELECT unnest(y1), y2 FROM normalized_u1_folded
  ),
  normalized_u1_y1_y2(y1, y2) AS (
   SELECT y1, unnest(y2) FROM normalized_u1_y1
  ),
  E3(y1, y2) AS (
    select y1, y2 from normalized_tt_y1_y2 except select y1, y2 from normalized_u1_y1_y2
  ),
  -- in general, for a set/bag difference involving K temporal attributes, we need K subqueries in the union
  -- even if this requires unnesting/nesting for each set operation, each operation is still O(n^2) and is performed at most K times as part of the union over n tuples
  -- for the FO approach, set difference would be over two relations of size O(n^(2^k))
  E3_folded_pre(y1, y2) AS (
    SELECT (R::RECORD).I1, (R::RECORD).I2 FROM
      (SELECT c_interval_set_diff(R1.y1, R1.y2, R2.y1, R2.y2)
       FROM normalized_tt_folded R1, normalized_u1_folded AS R2) AS F(R)
  ),
  -- difference (and intersection) requires that we check for emptiness of the resulting intervals
  E3_folded_post(y1, y2) AS (
    select y1, y2 from E3_folded_pre where array_lower(y1,1) IS NOT NULL AND array_lower(y2,1) IS NOT NULL
  )
  SELECT * FROM normalized_Ryt_folded
  SELECT DISTINCT y1, y2 FROM (SELECT y1, unnest(y2) AS y2 FROM (SELECT unnest(y1) AS y1, y2 from E3_folded_post) AS F) AS G
  SELECT * FROM E3 order by (y1::c_interval).i_left NULLS FIRST, (y2::c_interval).i_left NULLS FIRST;

WITH
  Ryt(country, y1, y2) AS (
    SELECT country, years, (i_left, i_right)::c_interval
    FROM indep, c_interval_true() AS T(i_left, i_right)
  ),
  endpoints1(country, y1, y2) AS (
   SELECT
      country, 
      int_array_sort(int_array_agg(ARRAY[(y1::c_interval).i_left, (y1::c_interval).i_right])), 
      int_array_sort(int_array_agg(ARRAY[(y2::c_interval).i_left, (y2::c_interval).i_right]))
   FROM (SELECT country, y1, y2 FROM Ryt UNION SELECT country, y2, y1 FROM Ryt) AS SQ
   GROUP BY country),
  normalized_Ryt_folded(country, y1, y2) AS (
   SELECT 
     Ryt.country,
     c_interval_normalized_array((Ryt.y1::c_interval).i_left,(Ryt.y1::c_interval).i_right,NA.y1), 
     c_interval_normalized_array((Ryt.y2::c_interval).i_left,(Ryt.y2::c_interval).i_right,NA.y2)
   FROM Ryt INNER JOIN endpoints1 NA ON Ryt.country = NA.country
  )
  SELECT * FROM normalized_Ryt_folded
--SELECT country, unnest(y1) FROM endpoints1
SELECT country, 
       (SELECT normalized_set(Ryt.y1, EP.y1) FROM endpoints1 EP WHERE EP.country = Ryt.country) AS y1
       (SELECT normalized_set(Ryt.y2, EP.y2) FROM endpoints1 EP WHERE EP.country = Ryt.country) AS y2
FROM Ryt
SELECT country, 
       array_agg(y1) OVER (PARTITION BY country ORDER BY y1 ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS agg_y1,
       array_agg(y2) OVER (PARTITION BY country ORDER BY y2 ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS agg_y2
   FROM Ryt, (SELECT country, unnest(y1) FROM endpoints1 EP WHERE EP.country = Ryt.country)UNION SELECT country, y2, y1 FROM Ryt) AS SQ
GROUP BY country
SELECT *
FROM Ryt NATURAL JOIN 
         (SELECT 
             country, 
             array_agg(y1) OVER (PARTITION BY country ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS agg_y1
             array_agg(y2) OVER (PARTITION BY country ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS agg_y2
          FROM endpoints) AS R
WHERE array_upper(agg_y1, 1) = 2 AND array_upper(agg_y2, 1) = 2;

-- 2.2sec
select * from generate_series(1,5000000) AS A(I) WHERE I >= 10000 AND I < 50000;
-- 3.9sec
select * from generate_series(1,5000000) AS A(I);

SELECT A FROM 
  (SELECT array_agg(I) OVER (PARTITION BY true ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS A 
   FROM generate_series(1,100) AS R(I)) AS R
WHERE array_upper(R.A, 1) = 2;

SELECT 
  mbr,
  c_interval_normalized_array(L,R,
     c_interval_normal_array(array_sort(ARRAY[2001] || int_array_agg(ARRAY[L, R+1]) OVER w), 
                             array_sort(ARRAY[6999] || int_array_agg(ARRAY[R, L-1]) OVER w)))
FROM bindings 
WHERE mbr = 'mbm 11' AND L > 2000 AND R < 7000
WINDOW w AS (PARTITION BY mbr);

-- to keep set-level representation we have two options:
--   1) two-level normalization: normalize intervals then make the sets equal or disjoint
--      worst case is singleton sets, i.e., degrading into the equivalent one-level normalization
--      idea: for each set, generate a set of subsets: best = 1, worst = size of set
--   2) modify the compilation to handle normalized relations by incorporating operations on set of intervals
--      because any of the sets may "interfere" with any of the other sets, the set-level operations must be aggregates
--      for instance, a set/bag aggregate operation involving k temporal attributes takes k sets of intervals and outputs k sets of intervals
--      for instance, 1-union is an aggregate that takes a set of intervals and outputs a set of intervals
--      for instance, 2-union is an aggregate that takes a pair of sets of intervals and outputs a pair of sets of intervals

IF A subset C THEN C = C - A AND B = B U D

  -- 29 tuples (symmetric)
  SELECT * FROM E3 order by (y1::c_interval).i_left NULLS FIRST, (y2::c_interval).i_left NULLS FIRST;
  --13x13
  SELECT * FROM normalized_tt_folded;
  --169
  SELECT * FROM normalized_tt_y1_y2
  -- 56 tuples of varying y1 and y2 dimensions
  SELECT * FROM normalized_U1_folded;
  --244
  SELECT * FROM normalized_U1_y1_y2

--AxB - CxD => in general, cannot be encoded as a single tuple 
--  t1 = (A-C)xB           (all A elements not in C are normally paired with all B elements)
--  t2 = Ax(B-D)           (all A elements are normally paired with all B elements not in D)

{1,2,3}x{4,5,6} = ({1,3} U {2})x({4} U {5,6}) = {1,3}x{4} U {1,3}U{5,6} U {2}x{4} U {2}x{5,6}

-- AxB - CxD = (A-C)xB + Ax(B-D) - (A-C)x(B-D) = (A-C)xB + (A-(A-C))x(B-D) --> disjoint union!
{1,2,3}x{4,5,6} - {2,6}x{1,5,6} = ({1,2,3}-{2,6})x{4,5,6} U ({1,2,3}-({1,2,3}-{2,6}))x({4,5,6}-{1,5,6})
                                = {1,3}x{4,5,6} U ({1,2,3}-{1,3})x{4}
                                = {1,3}x{4,5,6} U {2}x{4}
                                = 
                                
SELECT * FROM (SELECT 
  unnest(ARRAY(SELECT (I1,I2) AS R FROM 
    c_interval_set_diff(ARRAY[(1,1),(2,2),(3,3)]::c_interval[],
                        ARRAY[(4,4),(5,5),(6,6)]::c_interval[],
                        ARRAY[(2,2),(6,6)]::c_interval[],
                        ARRAY[(1,1),(5,5),(6,6)]::c_interval[]) AS F)) AS R) AS R
                           
{1,2,3}x{4,5,6} - {2,6}x{1,5,6} = {1,3}x{4,5,6} U {2}x{4}

AxB - CxD = (A-C)xB + Ax(B-D) - (A-C)x(B-D) = AxD - CxD + AxB - AxD = AxB - CxD
{1,2,3}x{4,5,6} - {2}x{5,6} = ({1,3}x{4,5,6} U {1,2,3}x{4}) - (A-C)x(BxD)

                            = ({1,3}x{4} U {1,3}x{5,6} U {2}x{4} U {2}x{5,6}) - ({2}x{5,6})
                            = {1,3}x{4} U {1,3}x{5,6} U {2}x{4}
                            = ({1,3}x{4,5,6} U {1,2,3}x{4}) - {1,3}x{4}

-- Q4
-- SELECT country 
-- FROM indep, (SELECT MIN(year) as y0 FROM indep WHERE country = 'Slovakia') AS M
-- WHERE year < y0

WITH
  M(y0) AS (
    SELECT MIN((years::c_interval).i_left) FROM indep WHERE country = 'Slovakia'
  ),
  Q4(country) AS (
    SELECT DISTINCT country
    FROM indep, M
    WHERE (years::c_interval).i_left < y0
  )
  SELECT * FROM Q4;

-- Q5
-- SELECT country, count(year) AS years
-- FROM indep
-- WHERE 1900 <= year AND year < 2000

WITH
  M(country, years) AS (
    SELECT country, c_interval_intersect(years, (1900, 1999)::c_interval) 
    FROM indep WHERE c_interval_overlaps(years, (1900, 1999)::c_interval)
  ),
  Q5(country, years) AS (
    SELECT country, SUM((years::c_interval).i_right - (years::c_interval).i_left + 1)
    FROM M
    GROUP BY country
  )
  SELECT * FROM Q5;

-- Q6
-- SELECT year, count(country) AS numofc
-- FROM indep
-- GROUP BY year

WITH
  normalarrays_1(n_years) AS (
   SELECT
      c_interval_normal_array(c_interval_array_sort_left(int_array_agg(ARRAY[(years::c_interval).i_left, (years::c_interval).i_right+1])), 
                              c_interval_array_sort_right(int_array_agg(ARRAY[(years::c_interval).i_right, (years::c_interval).i_left-1])))
   FROM indep
  ),
  normalized_M(country, years) AS (
    SELECT country, c_interval_normalized_set((years::c_interval).i_left, (years::c_interval).i_right, n_years)
    FROM indep, normalarrays_1
  ),
  Q6(country, years) AS (
    SELECT years, COUNT(country) FROM normalized_M GROUP BY years
  )
  SELECT * FROM Q6;

CREATE OR REPLACE FUNCTION c_interval_true() RETURNS SETOF c_interval LANGUAGE SQL AS $BODY$ 
  SELECT (NULL, NULL)::c_interval;
$BODY$

CREATE OR REPLACE FUNCTION c_interval_overlaps(IN i1 c_interval, IN i2 c_interval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT COALESCE($1.i_left, $2.i_right, 0) <= COALESCE($2.i_right, $1.i_left, 0) AND 
         COALESCE($2.i_left, $1.i_right, 0) <= COALESCE($1.i_right, $2.i_left, 0);
$BODY$

CREATE OR REPLACE FUNCTION c_interval_intersect(IN i1 c_interval, IN i2 c_interval) RETURNS c_interval LANGUAGE SQL AS $BODY$ 
  -- requires: intervals must overlap
  SELECT GREATEST(COALESCE($1.i_left, $2.i_left)), LEAST(COALESCE($1.i_right, $2.i_right));
$BODY$

CREATE OR REPLACE FUNCTION c_interval_contains_left(IN p INT, IN i c_interval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($1 IS NULL AND $2.i_left IS NULL) OR
         ($1 IS NOT NULL AND $1 >= COALESCE($2.i_left, $1) AND $1 <= COALESCE($2.i_right, $1));
$BODY$

CREATE OR REPLACE FUNCTION c_interval_contains_right(IN p INT, IN i c_interval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($1 IS NULL AND $2.i_right IS NULL) OR
         ($1 IS NOT NULL AND $1 >= COALESCE($2.i_left, $1) AND $1 <= COALESCE($2.i_right, $1));
$BODY$

CREATE OR REPLACE FUNCTION c_interval_set_diff(IN A c_interval[], IN B c_interval[]) RETURNS c_interval[] LANGUAGE SQL AS $$
  -- requires: interval arrays must be normalized
  SELECT ARRAY(
               SELECT $1[i]
                  FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
               EXCEPT
               SELECT $2[j]
                  FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j
              );
$$;

CREATE OR REPLACE FUNCTION c_interval_set_diff(IN A c_interval[], IN B c_interval[], IN C c_interval[], IN D c_interval[]) RETURNS TABLE(I1 c_interval[], I2 c_interval[]) LANGUAGE SQL AS $$
  -- requires: interval arrays must be normalized
  -- AxB - CxD = (A-C)xB + Ax(B-D) - (A-C)x(B-D) = (A-C)xB + (A-(A-C))x(B-D) --> disjoint union!
  WITH
    -- (A - C)
    DIFF1(A_C) AS (
      SELECT ARRAY(
          SELECT $1[i]
            FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
          EXCEPT
          SELECT $3[j]
            FROM generate_series(array_lower($3,1), array_upper($3,1)) AS j
      )
    )
  SELECT A_C::c_interval[], $2 FROM DIFF1
  UNION 
  SELECT c_interval_set_diff($1, A_C), c_interval_set_diff($2,$4) FROM DIFF1;
$$;

SELECT array_lower('{}'::int[],1),array_upper('{}'::int[],1)

SELECT * FROM
  (VALUES
  (true,c_interval_overlaps((1,2),    (2,3))),             -- t overlaps((1, 2), (2,3))
  (true,c_interval_overlaps((1,2),    (2,NULL))),          -- t overlaps((1, 2), (2,ifty)) 
  (true,c_interval_overlaps((1,2),    (NULL,3))),          -- t overlaps((1, 2), (-ifty,3)) 
  (true,c_interval_overlaps((1,2),    (NULL,NULL))),       -- t overlaps((1, 2), (-ifty,ifty)) 
  (true,c_interval_overlaps((1,NULL), (2,3))),             -- t overlaps((1, ifty), (2,3)) 
  (true,c_interval_overlaps((1,NULL), (2,NULL))),          -- t overlaps((1, ifty), (2,ifty)) 
  (true,c_interval_overlaps((1,NULL), (NULL,3))),          -- t overlaps((1, ifty), (-ifty,3)) 
  (true,c_interval_overlaps((1,NULL), (NULL,NULL))),       -- t overlaps((1, ifty), (-ifty,ifty)) 
  (true,c_interval_overlaps((NULL,2), (2,3))),             -- t overlaps((-ifty, 2), (2,3)) 
  (true,c_interval_overlaps((NULL,2), (2,NULL))),          -- t overlaps((-ifty, 2), (2,ifty)) 
  (true,c_interval_overlaps((NULL,2), (NULL,3))),          -- t overlaps((-ifty, 2), (-ifty,3)) 
  (true,c_interval_overlaps((NULL,2), (NULL,NULL))),       -- t overlaps((-ifty, 2), (-ifty,ifty)) 
  (true,c_interval_overlaps((NULL,NULL), (2,3))),          -- t overlaps((-ifty, ifty), (2,3)) 
  (true,c_interval_overlaps((NULL,NULL), (2,NULL))),       -- t overlaps((-ifty, ifty), (2,ifty)) 
  (true,c_interval_overlaps((NULL,NULL), (NULL,3))),       -- t overlaps((-ifty, ifty), (-ifty,3)) 
  (true,c_interval_overlaps((NULL,NULL), (NULL,NULL))),    -- t overlaps((-ifty, ifty), (-ifty,ifty)) 

  (false,c_interval_overlaps((1,2),   (4,6))),             -- f overlaps((1, 2), (4,6))
  (false,c_interval_overlaps((1,2),   (4,NULL))),          -- f overlaps((1, 2), (4,ifty)) 
  (true,c_interval_overlaps((1,2),    (NULL,6))),          -- t overlaps((1, 2), (-ifty,6)) 
  (true,c_interval_overlaps((1,2),    (NULL,NULL))),       -- t overlaps((1, 2), (-ifty,ifty)) 
  (true,c_interval_overlaps((1,NULL), (4,6))),             -- t overlaps((1, ifty), (4,6)) 
  (true,c_interval_overlaps((1,NULL), (4,NULL))),          -- t overlaps((1, ifty), (4,ifty)) 
  (true,c_interval_overlaps((1,NULL), (NULL,6))),          -- t overlaps((1, ifty), (-ifty,6)) 
  (true,c_interval_overlaps((1,NULL), (NULL,NULL))),       -- t overlaps((1, ifty), (-ifty,ifty)) 
  (false,c_interval_overlaps((NULL,2),(4,6))),             -- f overlaps((-ifty, 2), (4,6)) 
  (false,c_interval_overlaps((NULL,2),(4,NULL))),          -- f overlaps((-ifty, 2), (4,ifty)) 
  (true,c_interval_overlaps((NULL,2), (NULL,6))),          -- t overlaps((-ifty, 2), (-ifty,6)) 
  (true,c_interval_overlaps((NULL,2), (NULL,NULL))),       -- t overlaps((-ifty, 2), (-ifty,ifty)) 
  (true,c_interval_overlaps((NULL,NULL), (4,6))),          -- t overlaps((-ifty, ifty), (4,6)) 
  (true,c_interval_overlaps((NULL,NULL), (4,NULL))),       -- t overlaps((-ifty, ifty), (4,ifty)) 
  (true,c_interval_overlaps((NULL,NULL), (NULL,6))),       -- t overlaps((-ifty, ifty), (-ifty,6)) 
  (true,c_interval_overlaps((NULL,NULL), (NULL,NULL)))     -- t overlaps((-ifty, ifty), (-ifty,ifty)) 
  ) AS V(expected, actual)
WHERE expected <> actual;


SELECT * FROM
  (VALUES
  (true,c_interval_contains_left(2,     (2,4))),
  (true,c_interval_contains_left(3,     (2,4))),
  (true,c_interval_contains_left(4,     (2,4))),
  (false,c_interval_contains_left(1,    (2,4))),
  (false,c_interval_contains_left(5,    (2,4))),
  (false,c_interval_contains_left(NULL, (2,4))),
  (true,c_interval_contains_left(2,     (NULL,4))),
  (true,c_interval_contains_left(3,     (NULL,4))),
  (true,c_interval_contains_left(4,     (NULL,4))),
  (true,c_interval_contains_left(1,    (NULL,4))),
  (false,c_interval_contains_left(5,    (NULL,4))),
  (true,c_interval_contains_left(NULL, (NULL,4))),
  (true,c_interval_contains_left(2,     (NULL,NULL))),
  (true,c_interval_contains_left(3,     (NULL,NULL))),
  (true,c_interval_contains_left(4,     (NULL,NULL))),
  (true,c_interval_contains_left(1,    (NULL,NULL))),
  (true,c_interval_contains_left(5,    (NULL,NULL))),
  (true,c_interval_contains_left(NULL, (NULL,NULL))),
  (true,c_interval_contains_right(2,     (2,4))),
  (true,c_interval_contains_right(3,     (2,4))),
  (true,c_interval_contains_right(4,     (2,4))),
  (false,c_interval_contains_right(1,    (2,4))),
  (false,c_interval_contains_right(5,    (2,4))),
  (false,c_interval_contains_right(NULL, (2,4))),
  (true,c_interval_contains_right(2,     (2,NULL))),
  (true,c_interval_contains_right(3,     (2,NULL))),
  (true,c_interval_contains_right(4,     (2,NULL))),
  (false,c_interval_contains_right(1,    (2,NULL))),
  (true,c_interval_contains_right(5,    (2,NULL))),
  (true,c_interval_contains_right(NULL, (2,NULL))),
  (true,c_interval_contains_right(2,     (NULL,NULL))),
  (true,c_interval_contains_right(3,     (NULL,NULL))),
  (true,c_interval_contains_right(4,     (NULL,NULL))),
  (true,c_interval_contains_right(1,    (NULL,NULL))),
  (true,c_interval_contains_right(5,    (NULL,NULL))),
  (true,c_interval_contains_right(NULL, (NULL,NULL)))
  ) AS V(expected, actual)
WHERE expected <> actual;

-- 20 tuples
select unnest(ARRAY[1,2,3,4,5]), unnest(ARRAY['A','B','C','D'])

-- 5 tuples
select unnest(ARRAY[1,2,3,4,5]), unnest(ARRAY['A','B','C','D','E'])

-- IMPORTANT NOTE: see about encoding intervals as half-open and collecting endpoints into a single array without
-- the need to add neighborhoods of points, namely, l-1 and r+1; the set of endpoints this way easily splits a given
-- interval.
