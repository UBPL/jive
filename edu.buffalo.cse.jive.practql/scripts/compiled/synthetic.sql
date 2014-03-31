/** 
 * ************************************************************
 *  REQUIRES: 
 * 
 *    1) a bindings relation is required for the 
 *       generate_random_bindings function
 *       
 *    2) calltree relation is required for the 
 *       generate_random_calltree function
 * 
 *  PROVIDES: 
 * 
 *    1) a collection of functions to create synthetic
 *       tables representing contour member values and
 *       call trees; 
 * 
 *    2) creation scripts for calltree and binding 
 *       relations
 * ************************************************************
 */

﻿DROP SCHEMA synthetic;
CREATE SCHEMA synthetic;
ALTER DEFAULT PRIVILEGES IN SCHEMA synthetic GRANT ALL ON TABLES TO public;
GRANT ALL ON SCHEMA synthetic TO public;

SET SEARCH_PATH TO synthetic,ctdb,public;

﻿select 1*3*5*7*3*5*2*2*5*10000::int

SELECT MIN(t_s), MAX(t_e), AVG(t_e - t_s), COUNT(DISTINCT (t_s, t_e)) FROM random_k
SELECT * FROM vrandom_250k limit 100

WITH r AS (SELECT t_s, t_e FROM vrandom_1m),
     ru AS (SELECT t_s AS T FROM r UNION SELECT t_e AS T FROM r)
  SELECT MIN(T), MAX(T), COUNT(T) FROM ru

CREATE TABLE random_1m AS 
SELECT oid, fid, val, t_s, t_e 
FROM
  (SELECT oid, fid, val, row_number() OVER () AS rn FROM triangle_1m) AS T 
  NATURAL JOIN (SELECT t_s, t_e, row_number() OVER () AS rn FROM vrandom_1m) AS R
 
CREATE OR REPLACE VIEW vrandom_500k AS
(SELECT 1::int AS oid, loc AS t_s, loc + width AS t_e FROM
  (SELECT FLOOR(random()*(slots - width + 1))::INT AS loc, width FROM 
    (SELECT slots, FLOOR(1 + random()*slots)::INT AS width
     FROM (SELECT b500k,unit500k AS slots FROM random_slots) R1, GENERATE_SERIES(1, (SELECT b500k FROM random_slots))) AS R) AS R);

CREATE OR REPLACE VIEW random_slots AS
SELECT
  space,
  b1k,
  space/b1k AS unit1k, 
  b3k,
  space/b3k AS unit3k, 
  b5k,
  space/b5k AS unit5k, 
  b7k,
  space/b7k AS unit7k, 
  b9k,
  space/b9k AS unit9k, 
  b15k,
  space/b15k AS unit15k, 
  b25k,
  space/b25k AS unit25k, 
  b50k,
  space/b50k AS unit50k, 
  b75k,
  space/b75k AS unit75k, 
  b100k,
  space/b100k AS unit100k, 
  b250k,
  space/b250k AS unit250k, 
  b500k,
  space/b500k AS unit500k, 
  b1m,
  space/b1m AS unit1m
FROM 
  (VALUES(315000000,1000,3000,5000,7000,9000,15000,25000,50000,75000,100000,250000,500000,1000000)) 
     AS R(space,    b1k, b3k, b5k, b7k, b9k, b15k, b25k, b50k, b75k, b100k, b250k, b500k, b1m);

-- returns a chain of edges (1,2), ..., (N-1,N)
CREATE OR REPLACE FUNCTION create_chain(N INT, OUT edge1 INT, OUT edge2 INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  i INT;
BEGIN
  FOR i IN 1..N-1 LOOP
    edge1 := i;
    edge2 := i+1;
    RETURN NEXT;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- returns a chain of edges (1,2), ..., (N-1,N)
CREATE OR REPLACE FUNCTION create_fanchain(N INT, fanOut INT, OUT edge1 INT, OUT edge2 INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  i INT;
  j INT;
BEGIN
  edge1 := 0;
  FOR i IN 1..fanOut LOOP
    edge2 := (i-1)*N + 1;  -- (0,1), (0,N+1), ..., (0,(fanOut-1)*N+1)
    RETURN NEXT;
  END LOOP;
  FOR i IN 1..fanOut LOOP
    edge1 := (i-1)*N + 1;  -- 1, N+1, 2N+1, (fanOut-1)*N+1
    FOR j IN 1..(N-1) LOOP
      edge2 := edge1 + 1;
      RETURN NEXT;
      edge1 := edge2;
    END LOOP;
  END LOOP;
  edge2 := N*fanOut + 1;
  FOR i IN 1..fanOut LOOP
    edge1 := i*N;
    RETURN NEXT;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- returns a complete acyclic graph of order N
CREATE OR REPLACE FUNCTION create_complete_graph(N INT, OUT edge1 INT, OUT edge2 INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  i INT;
  j INT;
BEGIN
  edge1 := 1;
  FOR i IN 1..N LOOP
    edge1 := i;             -- 1, 2, ..., N
    FOR j IN i+1..N LOOP
      edge2 := j;
      RETURN NEXT;
    END LOOP;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- returns a cycle of edges (1,2), ..., (N-1,N), (N,1)
CREATE OR REPLACE FUNCTION create_cycle(N INT, OUT edge1 INT, OUT edge2 INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  r RECORD;
BEGIN
  FOR r IN SELECT * FROM create_chain(N) LOOP
    edge1 := r.edge1;
    edge2 := r.edge2;
    RETURN NEXT;
  END LOOP;
  edge1 := edge2;
  edge2 := 1;
  RETURN NEXT;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- returns a complete tree with the given height and number of children per level
CREATE OR REPLACE FUNCTION create_tree(width INT, height INT, OUT edge1 INT, OUT edge2 INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  h INT;
  e1 INT;
  e2 INT;
BEGIN
  -- root node
  FOR e2 IN 1..width LOOP
    edge1 := 0;
    edge2 := e2;
    RETURN NEXT;
  END LOOP;
  -- other nodes
  FOR h IN 0..height LOOP
    FOR e1 IN width^h..(width^(h+1)-1) LOOP
      edge1 := e1;
      FOR e2 IN e1*width..(e1+1)*width-1 LOOP
        edge2 := e2+1;
        RETURN NEXT;
      END LOOP;
    END LOOP;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

﻿-- creates an interval sequence of size 'count', with each interval having an expected duration 'width'; 
-- the sequence starts at 'initial' and intervals are separated by an expected gap of 'delta'
CREATE OR REPLACE FUNCTION random_interval_sequence_by_count(IN initial INT, IN width INT, IN count INT, IN delta INT, OUT t_s INT, OUT t_e INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  w RECORD;
BEGIN
  t_s := initial + CEIL(RANDOM()*2*delta)::int;
  t_e := t_s + CEIL(RANDOM()*2*width)::int;
  RETURN NEXT;
  FOR w IN SELECT CEIL(RANDOM()*2*delta)::int AS del, CEIL(RANDOM()*2*width)::int AS len FROM GENERATE_SERIES(1, count - 1) LOOP
    t_s := t_e + 1 + w.del;
    t_e := t_e + 1 + w.del + w.len;
    RETURN NEXT;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- creates an interval sequence contained within the [initial, final] interval, where each interval 
-- in the sequence has an expected duration 'width' and is separated by an expected gap of 'delta'
CREATE OR REPLACE FUNCTION random_interval_sequence_by_interval(initial INT, final INT, width INT, delta INT, OUT t_s INT, OUT t_e INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  done BOOLEAN;
BEGIN
  t_s := 0;
  t_e := 0;
  done := (initial > final);
  WHILE (NOT done) LOOP
    IF (t_s = 0 AND t_e = 0) THEN
      t_s := initial;
    ELSE
      t_s := t_e + CEIL(RANDOM()*2*delta)::int;
    END IF;
    t_e := t_s + CEIL(1+RANDOM()*2*width)::int;
    IF (t_s > final) THEN
      t_s := final;
      t_e := final;
    ELSIF (t_e > final) THEN
      t_e := final;
    END IF;
    RETURN NEXT;
    done := t_e = final;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- initial.......: interval tree start
-- width.........: expected width of an interval node
-- delta.........: expected distance between adjoining endpoints (t_s/t_s and t_e/t_e for parent/child, t_e/t_s for siblings) 
-- maxChildren...: maximum number of nodes per level
-- maxDepth......: maximum depth of the tree
CREATE OR REPLACE FUNCTION create_random_interval_tree(initial INT, width INT, delta INT, maxChildren INT, maxDepth INT, OUT id INT, OUT t_s INT, OUT t_e INT) RETURNS SETOF RECORD AS $BODY$
DECLARE
  ptr INT;
  prob NUMERIC;
  ids INT[];
  children INT[];
  nextLeft INT[];
BEGIN
  ptr := 1;
  prob := 0.5;
  ids[1] := 2;
  nextLeft[1] = initial + CEIL(1+RANDOM()*2*delta)::int;
  FOR h IN 1..maxDepth LOOP
    children[h] = 0;
  END LOOP;
  t_e := initial;
  WHILE (ptr >= 1) LOOP
    -- MOVE UP: current level is complete, move up to close the subtree
    IF (children[ptr] = maxChildren) THEN
      ptr := ptr - 1;
    -- CLOSE SUBTREE: current internal node has a complete subtree
    ELSIF (ptr < maxDepth AND children[ptr+1] = maxChildren) THEN
        id := ids[ptr];
        t_s := nextLeft[ptr];
        t_e := nextLeft[ptr+1];
        RETURN NEXT;
        -- update current level
        ids[ptr] := ids[ptr+1] + 1;
        nextLeft[ptr] := t_e + CEIL(1+RANDOM()*2*delta)::int;
        children[ptr] := children[ptr] + 1;
        -- reset lower level
        children[ptr+1] := 0;
    -- current level is incomplete
    ELSE
      -- add a new node/subtree with some probability
      IF (RANDOM() > prob) THEN
        -- update current level
        children[ptr] := children[ptr] + 1;
      ELSE
        -- OPEN NEW SUBTREE
        IF (ptr < maxDepth) THEN
          -- initialize subtree
          children[ptr+1] := 0;
          ids[ptr+1] := ids[ptr] + 1;
          nextLeft[ptr+1] := nextLeft[ptr] + CEIL(1+RANDOM()*2*delta)::int;
          -- MOVE DOWN
          ptr := ptr + 1;
        ELSE 
          -- CREATE LEAF NODE
          id := ids[ptr];
          t_s := nextLeft[ptr];
          t_e := t_s + CEIL(1+RANDOM()*2*width)::int;
          RETURN NEXT;
          -- update current level
          ids[ptr] := ids[ptr] + 1;
          nextLeft[ptr] := t_e + CEIL(1+RANDOM()*2*delta)::int;
          children[ptr] := children[ptr] + 1;
        END IF;
      END IF;
    END IF;
  END LOOP;
  id := 1;
  t_s := initial;
  t_e := nextLeft[1];
  RETURN NEXT;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

/* tables for normalization tests */
-- DROP TABLE Bindings CASCADE;
-- DROP TABLE CallTrees CASCADE;

CREATE TABLE Bindings (
  env varchar(10) not null, -- containing environment
  mbr varchar(10) not null, -- member
  val varchar(10) not null, -- value
  t_s int not null,
  t_e int null,
  constraint Bindings_interval check (t_s < t_e)
);

CREATE TABLE CallTrees (
  thd int not null, -- thread of the call tree
  act varchar(20) not null, -- activation's call identifier
  mth varchar(10) not null, -- method name
  ctx varchar(10) not null, -- caller environment, e.g., class or instance
  t_s int not null,
  t_e int null,
  constraint CallTrees_interval check (t_s < t_e)
);

-- objects.......: number of objects to create
-- fields........: expected number of fields per object
-- assignments...: expected number of assignments per field
-- liveFrom......: expected object's lifespan start
-- lifeSpan......: expected object's lifespan duration (field assignments partition this lifespan)
CREATE OR REPLACE FUNCTION generate_random_bindings(objects INT, fields INT, assignments INT, liveFrom INT, lifeSpan INT) RETURNS void AS $BODY$
DECLARE
  rec RECORD;
BEGIN
  CREATE TEMPORARY TABLE temp_bindings ON COMMIT DROP AS
  SELECT E.E, E.t_s, E.t_e
  FROM 
    (SELECT N, CEIL(1+RANDOM()*2*liveFrom)::INT, 2*liveFrom + CEIL(1+RANDOM()*2*lifespan)::INT
     FROM GENERATE_SERIES(1, objects) AS E(N)) AS E(E,t_s,t_e);

  DELETE FROM Bindings;
  
  FOR rec IN SELECT E, t_s, t_e FROM temp_bindings LOOP
    FOR fld IN 1..CEIL(1+RANDOM()*2*fields) LOOP
      INSERT INTO Bindings(env, mbr, val, t_s, t_e) 
        SELECT
          'env ' || rec.E::text, -- environment identifier
          'mbm ' || fld::text,   -- member identifier
          'env ' || CEIL(RANDOM()*objects)::text, -- reference to some environment
          I.t_s,
          I.t_e
        FROM
          random_interval_sequence_by_interval(rec.t_s, rec.t_e, CEIL((rec.t_e - rec.t_s)/(1+RANDOM()*2*assignments))::INT, 0) AS I(t_s, t_e);
    END LOOP;
  END LOOP;
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

-- trees.........: number of trees to create
-- methods.......: expected number of methods called per tree
-- contexts......: expected number of contexts referenced per tree
-- calls.........: expected number of calls made per tree
-- liveFrom......: expected call tree's lifespan start
-- lifeSpan......: expected call tree's duration
CREATE OR REPLACE FUNCTION generate_random_calltree(treeNo INT, methods INT, contexts INT, callWidth INT, callDelta INT, liveFrom INT, maxChildren INT, maxDepth INT) RETURNS void AS $BODY$
DECLARE
  methodMax INT;
  contextMax INT;
BEGIN
  methodMax := CEIL(1+RANDOM()*2*methods);
  contextMax := CEIL(1+RANDOM()*2*contexts);
  INSERT INTO CallTrees(thd, act, mth, ctx, t_s, t_e) 
    SELECT
      treeNo,
      'act ' || treeNo::text || '.' || CT.CID::text, -- activation identifier
      'mth ' || CEIL(1+RANDOM()*methodMax)::text, -- method identifier
      'ctx ' || CEIL(1+RANDOM()*contextMax)::text, -- calling environment
      CT.t_s,
      CT.t_e
    FROM
      create_random_interval_tree(CEIL(1+RANDOM()*2*liveFrom)::INT, callWidth, callDelta, maxChildren, maxDepth) AS CT(CID, t_s, t_e);
  RETURN;
END; $BODY$ LANGUAGE plpgsql;

/** 
 * ************************************************************
 *  TEST CASES                    
 * ************************************************************
 */

-- materialize a chain
create table chain32 as select * from create_chain(32);
vacuum analyze chain32;

-- materialize a cycle
create table cycle24 as select * from create_cycle(24);
vacuum analyze cycle24;

-- materialize a fan chain
create table fanchain32_8 as select * from create_fanchain(32,8);
vacuum analyze fanchain32_8;

-- materialize a tree
create table tree4_8 as select * from create_tree(4,8);
create index ix_tree4_8_e1 on tree4_8(edge1);
create index ix_tree4_8_e2 on tree4_8(edge2);
vacuum analyze tree4_8;

-- define a graph view that maps to one of the materialized graphs above
create or replace view graph(edge1,edge2) as
  select edge1, edge2 from tree4_8;

-- transitive closure on the graph view-- no selection
create view tc(a,d) as
  with recursive tc(a,d) as (
    select edge1, edge2 from graph
    union select tc.a, g.edge2 from tc inner join graph g on tc.d = g.edge1
  )
  select a,d from tc;

-- transitive closure on the graph view-- data selection
create view tc2(a,d) as
  with recursive tc2(a,d) as (
    select edge1, edge2 from graph where edge1 = 50000 
    union select g.edge1, tc2.d from graph g inner join tc2 on tc2.a = g.edge2 and tc2.d = 350000
  )
  select a,d from tc;

-- select * from information_schema.columns where table_name='graph';
select * from tc2
select * from tc where a = 50000 and d <= 350000;
SELECT 1 , 5 FROM  graph A WHERE A.edge1 = 1 AND A.edge2 = 5 

-- ~46sec independently of the where condition
select * from tc where a = 50000 and d = 350000;
select * from graph where edge1 > 50000 and edge2 < 100000 order by edge1, edge2;
select min(edge1), min(edge2), max(edge1), max(edge2) from graph;

-- select * from create_random_bindings(10000, 15, 5, 5000, 5000);
-- VACUUM ANALYZE bindings;
-- select count(*) from bindings;
-- select * from bindings order by env, t_e desc, mbr limit 1000;

CREATE INDEX ix_bindings_mbr on bindings(mbr);
CREATE INDEX ix_bindings_env on bindings(env);
CREATE INDEX ix_bindings_l on bindings(t_s);
CREATE INDEX ix_bindings_r on bindings(t_e);

-- DELETE FROM bindings;

-- 6.2sec / 550K rows
-- SELECT * FROM generate_random_bindings(5000, 15, 5, 5000, 5000);
-- SELECT * FROM bindings

-- 20sec / 1.1M rows
-- SELECT * FROM generate_random_bindings(10000, 15, 5, 5000, 5000);

-- VACUUM ANALYZE bindings;

-- SELECT COUNT(*) from bindings;

-- 180ms
-- SELECT * FROM bindings ORDER BY env, t_s limit 1000;

-- depending on index selectivity, all queries below may use index scans instead of table scans
-- SELECT * FROM bindings WHERE t_s BETWEEN 5000 AND 7000; -- 150ms
-- SELECT * FROM bindings WHERE t_e BETWEEN 5000 AND 6000; -- 60ms
-- SELECT * FROM bindings WHERE t_s BETWEEN 5000 AND 6000 AND t_e BETWEEN 5000 AND 6000; -- 40ms
-- SELECT * FROM bindings WHERE t_s BETWEEN 5000 AND 6000 OR t_e BETWEEN 5000 AND 6000; -- 100ms
-- SELECT * FROM bindings WHERE t_s BETWEEN 5000 AND 6000 OR t_s BETWEEN 7000 AND 8000; -- 150ms
-- SELECT * FROM bindings WHERE t_e BETWEEN 5000 AND 6000 OR t_e BETWEEN 7000 AND 8000; -- 120ms

-- SELECT * FROM create_chain(100);
-- SELECT * FROM create_fanchain(10,5) ORDER BY edge1, edge2;
-- SELECT * FROM create_cycle(100);
-- SELECT * FROM create_tree(3,3) ORDER BY edge1, edge2;
-- SELECT * FROM create_tree(2,6) ORDER BY edge1, edge2;
-- SELECT * FROM random_interval_sequence_by_count(1, 40, 100, 10)
-- SELECT * FROM random_interval_sequence_by_interval(1, 100, 10, 0)
-- SELECT * FROM create_random_interval_tree(1, 90, 10, 5, 5) ORDER BY id

WITH RECURSIVE 
  g(e1, e2) AS ( SELECT * FROM create_tree(2,6) )
  select * from g AS g1 inner join g AS g2 ON g1.e2 = g2.e1 AND g1.e1 

WITH RECURSIVE 
  g(e1, e2) AS ( SELECT * FROM create_tree(2,6) ),
  r(e1, e2, p) AS ( SELECT e1, e2, e1::text || '->' || e2::text FROM g 
                    UNION SELECT r.e1, g.e2, p || '->' || g.e2::text  FROM r INNER JOIN g ON r.e2 = g.e1 )
  SELECT p FROM r ORDER BY p

WITH RECURSIVE 
  g(e1, e2) AS ( SELECT * FROM create_tree(2,6) ),
  r(e1, e2) AS ( SELECT e1, e2 FROM g 
                 UNION SELECT r.e1, g.e2 FROM r INNER JOIN g ON r.e2 = g.e1 )
  SELECT * FROM r ORDER BY e1, e2
