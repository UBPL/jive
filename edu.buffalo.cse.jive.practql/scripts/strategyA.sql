/************************************************************/

-- a list of 10 integer values, duplicates and nulls allowed
CREATE OR REPLACE VIEW tp_array_10 AS
  SELECT CASE WHEN RANDOM() < 0.1 THEN NULL ELSE CEIL(RANDOM()*1000)::int END AS value
  FROM generate_series(1, 10) AS R(A);

-- a list of 100 integer values, duplicates and nulls allowed
CREATE OR REPLACE VIEW tp_array_100 AS
  SELECT CASE WHEN RANDOM() < 0.1 THEN NULL ELSE CEIL(RANDOM()*10000)::int END AS value
  FROM generate_series(1, 100) AS R(A);

-- a list of 1000 integer values, duplicates and nulls allowed
CREATE OR REPLACE VIEW tp_array_1000 AS
  SELECT CASE WHEN RANDOM() < 0.1 THEN NULL ELSE CEIL(RANDOM()*100000)::int END AS value
  FROM generate_series(1, 1000) AS R(A);

-- a list of integer values, duplicates and nulls allowed
CREATE OR REPLACE VIEW tp_array AS
  SELECT value FROM tp_array_1000;

/************************************************************/

CREATE OR REPLACE FUNCTION array_sort(ANYARRAY, BOOLEAN = FALSE) RETURNS ANYARRAY LANGUAGE SQL AS $$
  -- sorts the input array, optionally discarding nulls
  SELECT ARRAY(
    SELECT 
      $1[s.i]
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    WHERE 
      -- not strict or discard nulls
      (NOT $2) OR ($1[s.i] IS NOT NULL)
    ORDER BY 1 NULLS FIRST
); $$;
-- non-strict: SELECT unnest(array_sort(array_agg(value))) FROM tp_array;
-- strict: SELECT unnest(array_sort(array_agg(value), true)) FROM tp_array;

CREATE OR REPLACE FUNCTION array_sort_distinct(ANYARRAY, BOOLEAN = FALSE) RETURNS ANYARRAY LANGUAGE SQL AS $$
  -- eliminates duplicates and sorts the input array, optionally discarding nulls
  SELECT ARRAY(
    SELECT DISTINCT
      $1[s.i]
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    WHERE 
      -- not strict or discard nulls
      (NOT $2) OR ($1[s.i] IS NOT NULL)
    ORDER BY 1 NULLS FIRST
); $$;
-- non-strict: SELECT unnest(array_sort_distinct(array_agg(value))) FROM tp_array;
-- strict: SELECT unnest(array_sort_distinct(array_agg(value), true)) FROM tp_array;

CREATE OR REPLACE FUNCTION normalizing_array_sort(INT[]) RETURNS INT[] LANGUAGE SQL AS $$
  -- eliminates duplicates and sorts the input array, also discarding nulls
  SELECT ARRAY(
    SELECT DISTINCT
      $1[s.i]
    FROM
      generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    WHERE 
      ($1[s.i] IS NOT NULL)
    ORDER BY 1 
); $$;
-- non-strict: SELECT unnest(normalizing_array_sort(array_agg(value))) FROM tp_array;

CREATE OR REPLACE FUNCTION normalizing_array_append(INT[], INT) RETURNS INT[] LANGUAGE SQL STRICT AS $$
  SELECT $1 || $2;
$$;
-- non-strict: SELECT unnest(normalizing_array_append(array_agg(value))) FROM tp_array;

CREATE OR REPLACE FUNCTION normalizing_endpoint_append(INT[], tp_interval) RETURNS INT[] LANGUAGE SQL STRICT AS $$
  SELECT $1 || ($2::tp_interval).s || ($2::tp_interval).e;
$$;

CREATE OR REPLACE FUNCTION normalizing_interval_append(INT[], INT[]) RETURNS INT[] LANGUAGE SQL STRICT AS $$
  SELECT $1 || $2;
$$;

CREATE OR REPLACE FUNCTION normalizing_set_append(INT[], INT[]) RETURNS INT[] LANGUAGE SQL STRICT AS $$
  SELECT $1 || $2;
$$;

CREATE OR REPLACE FUNCTION normalizing_set_append(INT[], INT, INT) RETURNS INT[] LANGUAGE SQL STRICT AS $$
  SELECT $1 || $2 || $3;
$$;

CREATE OR REPLACE FUNCTION normalizing_unnest(ANYARRAY) RETURNS SETOF ANYELEMENT LANGUAGE SQL STRICT AS $$
  SELECT DISTINCT $1[S.i] FROM generate_series(array_lower($1, 1), array_upper($1, 1)) AS S(i);
$$;

CREATE OR REPLACE FUNCTION normalizing_unnest(ANYARRAY) RETURNS SETOF ANYELEMENT LANGUAGE SQL STRICT AS $$
  SELECT NULL
  UNION ALL SELECT DISTINCT $1[S.i] FROM generate_series(array_lower($1, 1), array_upper($1, 1)) AS S(i)
  UNION ALL SELECT NULL;
$$;

CREATE OR REPLACE FUNCTION normalizing_unnest(INT, INT, tp_interval[]) RETURNS SETOF tp_interval LANGUAGE SQL STRICT AS $$
  SELECT $3[S.i] FROM generate_series(array_lower($3, 1), array_upper($3, 1)) AS S(I)
  WHERE ($3[S.i]::tp_interval).s >= $1 AND ($3[S.i]::tp_interval).e <= $2;
$$;

CREATE OR REPLACE FUNCTION normalizing_interval_set(IN INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$
    SELECT ARRAY(
      SELECT (segment[1], segment[2])::tp_interval
      FROM (SELECT array_agg(T) OVER (ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
            FROM normalizing_unnest($1) AS R(T)) AS A
      WHERE array_upper(segment, 1) = 2)
$BODY$

CREATE OR REPLACE FUNCTION normalizing_interval_set(IN INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$
    SELECT ARRAY(
      SELECT (NULL, MIN(T))::tp_interval FROM normalizing_unnest($1) AS R(T)
      UNION 
      SELECT (segment[1], segment[2])::tp_interval
      FROM (SELECT array_agg(T) OVER (ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
            FROM normalizing_unnest($1) AS R(T)) AS A
      WHERE array_upper(segment, 1) = 2
      UNION SELECT (MAX(T), NULL)::tp_interval FROM normalizing_unnest($1) AS R(T));
$BODY$

CREATE OR REPLACE FUNCTION normalizing_interval_set(IN INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$
    SELECT ARRAY(
      SELECT (segment[1], segment[2])::tp_interval
      FROM (SELECT array_agg(T) OVER (ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
            FROM normalizing_unnest($1) AS R(T)) AS A
      WHERE array_upper(segment, 1) = 2)
$BODY$

CREATE OR REPLACE FUNCTION normalizing_intervals(IN INT[]) RETURNS SETOF tp_interval LANGUAGE SQL AS $BODY$
      SELECT (segment[1], segment[2])::tp_interval
      FROM (SELECT array_agg(T) OVER (ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
            FROM normalizing_unnest($1) AS R(T)) AS A
      WHERE array_upper(segment, 1) = 2
$BODY$

CREATE FUNCTION normalizing_interval_array(IN PTs INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$
   SELECT ARRAY(SELECT (s, e)::tp_interval FROM normalizing_interval_set2($1) AS R(s,e));
$BODY$

CREATE OR REPLACE FUNCTION normalizing_interval_set2(IN PTs INT[], OUT _i tp_interval) RETURNS SETOF tp_interval LANGUAGE plpgsql AS $BODY$
DECLARE
  pre INT;
  cur INT;
  max INT;
BEGIN
  max := array_upper(PTs,1);
  pre := array_lower(PTs,1);
  cur := pre + 1;
  WHILE (cur <= max) LOOP
    _i.s := PTs[pre];
    _i.e := PTs[cur];
    RETURN NEXT;
    pre := cur;
    cur := cur + 1;
  END LOOP;
  RETURN;
END $BODY$

CREATE AGGREGATE normalized_set(tp_interval) (
  SFUNC=normalizing_endpoint_append,
  STYPE=INT[],
  INITCOND='{}',
  FINALFUNC=normalizing_interval_set
);
-- SELECT normalized_set((s,e)::tp_interval) FROM tp_interval_array;

CREATE AGGREGATE agg_normalized_intervals(INT[]) (
  SFUNC=normalizing_set_append,
  STYPE=INT[],
  INITCOND='{}',
  FINALFUNC=normalizing_interval_set
);

CREATE AGGREGATE agg_normalized_intervals2(INT, INT) (
  SFUNC=normalizing_set_append,
  STYPE=INT[],
  INITCOND='{}',
  FINALFUNC=normalizing_interval_set
);

CREATE AGGREGATE agg_normalized_intervals3(INT, INT) (
  SFUNC=normalizing_set_append,
  STYPE=INT[],
  INITCOND='{}',
  FINALFUNC=normalizing_interval_array
);

/************************************************************/

CREATE OR REPLACE FUNCTION array_append(INT[], INT[]) RETURNS INT[] LANGUAGE SQL AS $BODY$ 
  -- takes as input two arrays and appends the second to the end of the first
  SELECT $1 || $2;
$BODY$

CREATE AGGREGATE endpoint_set(INT[]) (
  SFUNC=array_append,
  STYPE=INT[],
  INITCOND='{}',
  FINALFUNC=normalizing_array_sort
);
-- SELECT endpoint_set(ARRAY[value]) FROM tp_array;

CREATE OR REPLACE FUNCTION array_append(INT[], INT) RETURNS INT[] LANGUAGE SQL AS $BODY$ 
  -- takes as input an array and an element and appends the element to the end of the array
  SELECT $1 || $2;
$BODY$

CREATE AGGREGATE endpoint_set(INT) (
  SFUNC=array_append,
  STYPE=INT[],
  INITCOND='{}',
  FINALFUNC=normalizing_array_sort
);
-- SELECT endpoint_set(value) FROM tp_array;

-- TODO: create an aggregate that collects the endpoints and produces the normalizing_set in one pass
-- it should use the array_append for collecting the set of endpoints
-- as the final function, it should eliminate duplicates and apply the window function on sorted partitions
-- this will likely eliminate one materialization step in the plans

/************************************************************/

-- drop interval type
DROP TYPE tp_interval CASCADE;

-- create interval type-- constraints cannot be defined here
CREATE TYPE tp_interval AS (s INT, e INT);	

-- select a single interval
SELECT (1,2)::tp_interval AS I;

-- select the left interval endpoint
SELECT ((1,2)::tp_interval).s;

-- select the right interval endpoint
SELECT ((1,2)::tp_interval).e;

-- select a constant bag of intervals
SELECT I, (I::tp_interval).s, (I::tp_interval).e
FROM (VALUES ((1,10)::tp_interval), ((5,15)::tp_interval), ((5,15)::tp_interval), ((15,25)::tp_interval)) AS F(I);

-- select a constant set of intervals
SELECT DISTINCT I, (I::tp_interval).s, (I::tp_interval).e
FROM (VALUES ((1,10)::tp_interval), ((5,15)::tp_interval), ((5,15)::tp_interval), ((15,25)::tp_interval)) AS F(I);

/************************************************************/

-- a list of tp_interval values, duplicates and nulls allowed
CREATE OR REPLACE VIEW tp_interval_array AS
  SELECT L.value AS s, CEIL(COALESCE(L.value, 0) + 1 + RANDOM() * 1000)::int AS e
  FROM tp_array_1000 L;

-- SELECT s, e FROM tp_interval_array ORDER BY s NULLS FIRST, e NULLS FIRST
-- SELECT array_agg((s, e)::tp_interval) AS value FROM tp_interval_array
-- SELECT endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1]) FROM tp_interval_array

CREATE OR REPLACE FUNCTION normalizing_intervals(IN Ls INT[], IN Rs INT[], OUT _i tp_interval) RETURNS SETOF tp_interval AS $BODY$
-- closed interval semantics
-- builds a normalizing interval set
-- assumes Ls and Rs are sorted in ascending ordered and have no NULLs
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
-- SELECT value FROM (SELECT normalizing_intervals(endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1])) FROM tp_interval_array) AS R(value)
-- SELECT value FROM (SELECT normalizing_intervals(endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1])) FROM tp_interval_array) AS R(value) WHERE (value::tp_interval).s <= (value::tp_interval).e

CREATE OR REPLACE FUNCTION normalizing_interval_set(IN Ls INT[], IN Rs INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$
  SELECT ARRAY(SELECT (s, e)::tp_interval FROM normalizing_intervals($1, $2) AS R(s, e));
$BODY$
-- SELECT normalizing_interval_set(endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1])) FROM tp_interval_array

CREATE OR REPLACE FUNCTION normalized_intervals(INT, INT, tp_interval[]) RETURNS SETOF tp_interval LANGUAGE SQL AS $BODY$ 
  -- overlapping is determined using closed interval semantics
  -- decomposes the input interval into a set of normalized intervals based on the set of input intervals
  SELECT GREATEST($1, s), LEAST($2, e) 
  FROM unnest($3) AS F(s, e)
  WHERE $1 <= e AND s <= $2; -- overlaps
$BODY$ 

CREATE OR REPLACE FUNCTION normalized_interval_set(INT, INT, tp_interval[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$ 
  SELECT ARRAY(SELECT (s, e)::tp_interval FROM normalized_intervals($1, $2, $3) AS R(s, e));
$BODY$ 

CREATE OR REPLACE FUNCTION normalizing_intervals(IN EPs INT[], OUT _i tp_interval) RETURNS SETOF tp_interval LANGUAGE SQL AS $BODY$
    SELECT (segment[1], segment[2])::tp_interval
    FROM (SELECT array_agg(T) OVER (ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
          FROM unnest($1) AS R(T)) AS A
    WHERE array_upper(segment, 1) = 2
$BODY$

CREATE OR REPLACE FUNCTION normalizing_interval_set(IN EPs INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$
  SELECT ARRAY(SELECT (s, e)::tp_interval FROM normalizing_intervals($1) AS R(s, e));
$BODY$

CREATE OR REPLACE FUNCTION normalized_intervals(IN L INT, IN R INT, IN _eps INT[], OUT _iout tp_interval) RETURNS SETOF tp_interval AS $BODY$
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
      _iout.s := _eps[ptr];
      _iout.e := _eps[ptr+1];
      RETURN NEXT;
    END IF;
  END LOOP;
  IF (L IS NULL) THEN
    _iout.s := NULL;
    _iout.e := val_L;
    RETURN NEXT;
  END IF;
  IF (R IS NULL) THEN
    _iout.s := val_R;
    _iout.e := NULL;
    RETURN NEXT;
  END IF;
  RETURN;
END; $BODY$ LANGUAGE plpgsql; 

CREATE OR REPLACE FUNCTION normalized_interval_set(INT, INT, INT[]) RETURNS tp_interval[] LANGUAGE SQL AS $BODY$ 
  SELECT ARRAY(SELECT (s, e)::tp_interval FROM normalized_intervals($1, $2, $3) AS R(s, e));
$BODY$ 

CREATE OR REPLACE FUNCTION tp_interval_min(IN tp_interval[]) RETURNS INT LANGUAGE SQL AS $BODY$ 
  -- semantics: half-open intervals
  -- returns the smallest instant contained in the interval array
  SELECT MIN(s) FROM unnest($1) AS F(s, e); 
$BODY$ 
-- SELECT tp_interval_min(normalizing_interval_set(endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1]))) FROM tp_interval_array

CREATE OR REPLACE FUNCTION tp_interval_max(IN tp_interval[]) RETURNS INT LANGUAGE SQL AS $BODY$ 
  -- semantics: half-open intervals
  -- returns the largest instant contained in the interval array
  SELECT MAX(e) - 1 FROM unnest($1) AS F(s, e); 
$BODY$ 
-- SELECT tp_interval_max(normalizing_interval_set(endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1]))) FROM tp_interval_array

CREATE OR REPLACE FUNCTION tp_interval_count(IN tp_interval[]) RETURNS BIGINT LANGUAGE SQL AS $BODY$ 
  -- semantics: half-open intervals
  -- returns the duration of the intervals in the array
  SELECT SUM(e - s) FROM unnest($1) AS F(s, e); 
$BODY$ 
-- SELECT tp_interval_count(normalizing_interval_set(endpoint_set(ARRAY[s, e+1]), endpoint_set(ARRAY[e, s-1]))) FROM tp_interval_array

/************************************************************/

-- 6.5K, 420ms
-- endpoints computes two sets on a single pass
-- IMin materialized unfolded
-- final query computes unfolded normalized intervals
WITH
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, times_s, times_e) AS (
    SELECT 
      env,
      endpoint_set(ARRAY[2000, time_s, time_e + 1]), 
      endpoint_set(ARRAY[6999, time_e, time_s - 1])
    FROM vbindings 
    GROUP BY env
  ),
  Imin(env, min_interval) AS (
    SELECT env, normalizing_intervals(times_s, times_e) FROM endpoints
  )
  SELECT env, GREATEST(time_s, (min_interval::tp_interval).s), LEAST(time_e, (min_interval::tp_interval).e)
  FROM vbindings B NATURAL JOIN IMin
  WHERE time_s <= (min_interval::tp_interval).e AND (min_interval::tp_interval).s <= time_e;

-- 8.2K, 530ms
-- endpoints computes two sets on a single pass
-- IMin materialized folded
-- final query computes unfolded normalized intervals
WITH
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, times_s, times_e) AS (
    SELECT 
      env,
      endpoint_set(ARRAY[2000, time_s, time_e + 1]), 
      endpoint_set(ARRAY[6999, time_e, time_s - 1])
    FROM vbindings 
    GROUP BY env
  ),
  Imin(env, intervals) AS (
    SELECT env, normalizing_interval_set(times_s, times_e) FROM endpoints
  )
  SELECT env, normalized_intervals(time_s, time_e, intervals)
  FROM vbindings B NATURAL JOIN IMin;

-- 8.2K, 800ms
-- endpoints computes two sets on a single pass
-- IMin materialized folded
-- final query computes folded normalized intervals
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, times_s, times_e) AS (
    SELECT 
      env,
      endpoint_set(ARRAY[2000, time_s, time_e + 1]), 
      endpoint_set(ARRAY[6999, time_e, time_s - 1])
    FROM vbindings 
    GROUP BY env
  ),
  Imin(env, intervals) AS (
    SELECT env, normalizing_interval_set(times_s, times_e) FROM endpoints
  )
  SELECT env, normalized_interval_set(time_s, time_e, intervals)
  FROM vbindings B NATURAL JOIN IMin;

/************************************************************/

-- 6.4K, 300ms
-- endpoints computes one set on a single pass
-- IMin materialized unfolded using window function
-- final query computes unfolded normalized intervals
WITH
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, times) AS (
    SELECT 
      env,
      endpoint_set(ARRAY[2000, 7000, time_s, time_e])
    FROM vbindings 
    GROUP BY env
  ),
  Imin(env, min_interval) AS (
    SELECT env, normalizing_intervals(times) FROM endpoints
  )
  SELECT env, GREATEST(time_s, (min_interval::tp_interval).s), LEAST(time_e, (min_interval::tp_interval).e)
  FROM vbindings B NATURAL JOIN IMin
  WHERE time_s <= (min_interval::tp_interval).e AND (min_interval::tp_interval).s <= time_e;

-- 8.1K, 430ms
-- endpoints computes one set on a single pass
-- IMin materialized folded using window function
-- final query computes unfolded normalized intervals
WITH
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, times) AS (
    SELECT 
      env,
      endpoint_set(ARRAY[2000, 7000, time_s, time_e])
    FROM vbindings 
    GROUP BY env
  ),
  Imin(env, intervals) AS (
    SELECT env, normalizing_interval_set(times) FROM endpoints
  )
  SELECT env, normalized_intervals(time_s, time_e, intervals)
  FROM vbindings B NATURAL JOIN IMin;

-- 8.1K, 715ms
-- endpoints computes one set on a single pass
-- IMin materialized unfolded using window function
-- final query computes folded normalized intervals
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, times) AS (
    SELECT 
      env,
      endpoint_set(ARRAY[2000, 7000, time_s, time_e])
    FROM vbindings 
    GROUP BY env
  ),
  Imin(env, intervals) AS (
    SELECT env, normalizing_interval_set(times) FROM endpoints
  )
  SELECT env, normalized_interval_set(time_s, time_e, intervals)
  FROM vbindings B NATURAL JOIN IMin;

-- 5.9K, 6913 tuples, 130ms
WITH
  normalizing_sets(env, interval_set) AS (
     SELECT env, agg_normalized_intervals(ARRAY[time_s, time_e]) OVER (PARTITION BY env)
     FROM Bindings 
     WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  )
SELECT env, interval_set FROM normalizing_sets

-- 7K, 490ms, 136K tuples, 480ms => 280K tuples per second
SELECT env, time_s, time_e, (min_interval::tp_interval).s, (min_interval::tp_interval).e FROM 
  (SELECT env, time_s, time_e, 
          unnest(agg_normalized_intervals(ARRAY[GREATEST(2000, time_s), LEAST(time_e, 7000)]) OVER (PARTITION BY env))
   FROM Bindings 
   WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000) AS R(env, time_s, time_e, min_interval)
WHERE (min_interval::tp_interval).s >= time_s AND (min_interval::tp_interval).e <= time_e
ORDER BY env, time_s, time_e, (min_interval::tp_interval).s, (min_interval::tp_interval).e 

-- 7K, 500ms
SELECT env, (min_interval::tp_interval).s, (min_interval::tp_interval).e FROM 
  (SELECT env, time_s, time_e, 
          unnest(agg_normalized_intervals2(GREATEST(2000, time_s), LEAST(time_e, 7000)) OVER (PARTITION BY env))
   FROM Bindings 
   WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000) AS R(env, time_s, time_e, min_interval)
WHERE (min_interval::tp_interval).s >= time_s AND (min_interval::tp_interval).e <= time_e
ORDER BY env, s, e

-- 7K, 1.1sec (normalizing set as a pgplsql function instead of sql)
SELECT env, (min_interval::tp_interval).s, (min_interval::tp_interval).e FROM 
  (SELECT env, time_s, time_e, 
          unnest(agg_normalized_intervals3(GREATEST(2000, time_s), LEAST(time_e, 7000)) OVER (PARTITION BY env))
   FROM Bindings 
   WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000) AS R(env, time_s, time_e, min_interval)
WHERE (min_interval::tp_interval).s >= time_s AND (min_interval::tp_interval).e <= time_e
ORDER BY env, s, e

WITH
  original(env, t_s, t_e) AS (
     SELECT env, GREATEST(2000, time_s), LEAST(7000, time_e)
     FROM Bindings 
     WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  )
SELECT env, (min_t::tp_interval).s, (min_t::tp_interval).e FROM 
  (SELECT env, t_s, t_e, 
          unnest(agg_normalized_intervals2(t_s, t_e) OVER (PARTITION BY env))
   FROM original) AS R(env, t_s, t_e, min_t)
WHERE (min_t::tp_interval).s >= t_s AND (min_t::tp_interval).e <= t_e
ORDER BY env, s, e

-- 7K, 730ms
SELECT env, (min_interval::tp_interval).s, (min_interval::tp_interval).e 
FROM Bindings NATURAL JOIN
  (SELECT env, 
          unnest(agg_normalized_intervals(ARRAY[GREATEST(2000, time_s), LEAST(time_e, 7000)]))
   FROM Bindings 
   WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
   GROUP BY env) AS R(env, min_interval)
WHERE (min_interval::tp_interval).s >= time_s AND (min_interval::tp_interval).e <= time_e
ORDER BY env, s, e

-- 9.7K/6.1K, 8.8M tuples, 17.8sec/18sec (before/after indexing) => 500K tuples per second
SELECT mbr, (min_interval::tp_interval).s, (min_interval::tp_interval).e FROM 
  (SELECT mbr, time_s, time_e, 
          unnest(agg_normalized_intervals(ARRAY[GREATEST(2000, time_s), LEAST(time_e, 7000)]) OVER (PARTITION BY mbr))
   FROM Bindings 
   WHERE mbr < 'mbm 10' AND 2000 < time_e AND time_s < 7000) AS R(mbr, s, e, min_interval)
WHERE (min_interval::tp_interval).s >= s AND (min_interval::tp_interval).e <= e

--9.5sec
--DROP INDEX ix_bindings_mbr on bindings(mbr);
--DROP INDEX ix_bindings_se on bindings(time_s, time_e);

WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings
  )
 SELECT DISTINCT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B 
    NATURAL JOIN (
      SELECT env, segment[1] AS L, segment[2] AS R
      FROM (SELECT env, array_agg(T) OVER (PARTITION BY env ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
            FROM endpoints) AS IMin
      WHERE array_upper(segment, 1) = 2
    ) BNS
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- 460ms
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings 
    WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings 
  ),
  Imin(env, L, R) AS (
    SELECT env, segment[1] AS L, segment[2] AS R
    FROM (SELECT env, array_agg(T) OVER (PARTITION BY env ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
          FROM endpoints WHERE 2000 <= T AND T <= 7000) AS IMin
    WHERE array_upper(segment, 1) = 2
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS
 WHERE 
   BNS.L >= B.time_s AND BNS.R <= B.time_e
 ORDER BY env, time_s, time_e, i_left, i_right;

-- 455ms
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings 
    WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  Imin(env, L, R) AS (
    SELECT env, segment[1] AS L, segment[2] AS R
    FROM (SELECT env, array_agg(T) OVER (PARTITION BY env ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
          FROM (SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings) AS EPs(env, T)
          WHERE 2000 <= T AND T <= 7000) AS IMin
    WHERE array_upper(segment, 1) = 2
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS
 WHERE 
   BNS.L >= B.time_s AND BNS.R <= B.time_e
 ORDER BY env, time_s, time_e, i_left, i_right;

-- 455ms
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings 
    WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN (
       SELECT env, segment[1] AS L, segment[2] AS R
       FROM (SELECT env, array_agg(T) OVER (PARTITION BY env ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
             FROM (SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings) AS EPs(env, T)
             WHERE 2000 <= T AND T <= 7000) AS IMin
       WHERE array_upper(segment, 1) = 2
    ) BNS
 WHERE 
   BNS.L >= B.time_s AND BNS.R <= B.time_e
 ORDER BY env, time_s, time_e, i_left, i_right;


/************************************************************/

--
-- The following table and its data are from Jan's survey on temporal databases (1995)
--
CREATE TABLE Indep (
  country VARCHAR(100) NOT NULL, 
  capital VARCHAR(100) NOT NULL, 
  years tp_interval,
  CONSTRAINT pk_indep PRIMARY KEY(country, capital, years),
  CONSTRAINT ck_interval CHECK((years::tp_interval).s < (years::tp_interval).e)
);
DELETE FROM Indep;
INSERT INTO Indep(country, capital, years)
VALUES ('Czech Kingdom', 'Prague', (1198, 1621)),
       ('Czechoslovakia', 'Prague', (1918, 1939)),
       ('Czechoslovakia', 'Prague', (1945, 1993)),
       ('Czech Republic', 'Prague', (1995, NULL)),
       ('Slovakia', 'Bratislava', (1940, 1945)),
       ('Slovakia', 'Bratislava', (1993, NULL)),
       ('Poland', 'Gniezo', (1025, 1040)),
       ('Poland', 'Cracow', (1040, 1596)),
       ('Poland', 'Warsaw', (1596, 1795)),
       ('Poland', 'Warsaw', (1918, 1939)),
       ('Poland', 'Warsaw', (1945, NULL));

-- Example 3.3 from Toman's SQL/TP paper
-- Q1
-- SELECT r1.country
-- FROM indep r1, indep r2
-- WHERE r2.country = 'Czech Kingdom' and r1.year = r2.year

-- ~1ms
SELECT DISTINCT r1.country FROM Indep r1, Indep r2
WHERE r2.country  = 'Czech Kingdom' AND tp_interval_overlaps(r1.years, r2.years);

-- Q2
-- SELECT t AS year FROM true 
-- EXCEPT SELECT year FROM Indep

-- ~10ms
WITH 
  normalizing(min_interval) AS (
    SELECT DISTINCT min_interval FROM 
      (SELECT s, e, unnest(agg_normalized_intervals2(s, e) OVER ())
       FROM (SELECT (years::tp_interval).s, (years::tp_interval).e FROM indep 
             UNION SELECT s, e FROM tp_interval_true() AS T(s, e)) AS T) AS R(s, e, min_interval)
  ),
  normalized(years) AS (
    SELECT min_interval FROM tp_interval_true() I NATURAL JOIN normalizing N
    WHERE tp_interval_overlaps(min_interval, (I.s, I.e)::tp_interval)
    EXCEPT
    SELECT min_interval FROM indep I NATURAL JOIN normalizing N
    WHERE tp_interval_overlaps(min_interval, I.years)
  )
  SELECT (years::tp_interval).s, (years::tp_interval).e FROM normalized;
  
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
    SELECT country, years, (i_left, i_right)::tp_interval
    FROM indep, tp_interval_true() AS T(i_left, i_right)
  ),
  normalarrays_1(country, y1, y2) AS (
   SELECT
      country, 
      tp_interval_normal_array(tp_interval_array_sort_left(int_array_agg(ARRAY[(y1::tp_interval).s, (y1::tp_interval).e+1])), 
                              tp_interval_array_sort_right(int_array_agg(ARRAY[(y1::tp_interval).e, (y1::tp_interval).s-1]))),
      tp_interval_normal_array(tp_interval_array_sort_left(int_array_agg(ARRAY[(y2::tp_interval).s, (y2::tp_interval).e+1])), 
                              tp_interval_array_sort_right(int_array_agg(ARRAY[(y2::tp_interval).e, (y2::tp_interval).s-1])))
   FROM (SELECT country, y1, y2 FROM Ryt UNION SELECT country, y2, y1 FROM Ryt) AS SQ
   GROUP BY country),
  normalized_Ryt_folded(country, y1, y2) AS (
   SELECT 
     Ryt.country,
     tp_interval_normalized_array((Ryt.y1::tp_interval).s,(Ryt.y1::tp_interval).e,NA.y1), 
     tp_interval_normalized_array((Ryt.y2::tp_interval).s,(Ryt.y2::tp_interval).e,NA.y2)
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
   SELECT (T1.s, T1.e)::tp_interval, (T2.s, T2.e)::tp_interval
   FROM tp_interval_true() AS T1(i_left, i_right), tp_interval_true() AS T2(i_left, i_right)
  ),
  normalarrays_2(y1, y2) AS (
   SELECT
      tp_interval_normal_array(tp_interval_array_sort_left(int_array_agg(ARRAY[(y1::tp_interval).s, (y1::tp_interval).e+1])), 
                              tp_interval_array_sort_right(int_array_agg(ARRAY[(y1::tp_interval).e, (y1::tp_interval).s-1]))),
      tp_interval_normal_array(tp_interval_array_sort_left(int_array_agg(ARRAY[(y2::tp_interval).s, (y2::tp_interval).e+1])), 
                              tp_interval_array_sort_right(int_array_agg(ARRAY[(y2::tp_interval).e, (y2::tp_interval).s-1])))
   FROM (SELECT y1, y2 FROM U1 UNION SELECT y1, y2 FROM TT) AS SQ),
  normalized_tt_folded(y1, y2) AS (
   SELECT 
     tp_interval_normalized_array((TT.y1::tp_interval).s,(TT.y1::tp_interval).e,na.y1), 
     tp_interval_normalized_array((TT.y2::tp_interval).s,(TT.y2::tp_interval).e,na.y2)
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
     tp_interval_normalized_array((U1.y1::tp_interval).s,(U1.y1::tp_interval).e,na.y1), 
     tp_interval_normalized_array((U1.y2::tp_interval).s,(U1.y2::tp_interval).e,na.y2)
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
      (SELECT tp_interval_set_diff(R1.y1, R1.y2, R2.y1, R2.y2)
       FROM normalized_tt_folded R1, normalized_u1_folded AS R2) AS F(R)
  ),
  -- difference (and intersection) requires that we check for emptiness of the resulting intervals
  E3_folded_post(y1, y2) AS (
    select y1, y2 from E3_folded_pre where array_lower(y1,1) IS NOT NULL AND array_lower(y2,1) IS NOT NULL
  )
  SELECT * FROM normalized_Ryt_folded
  SELECT DISTINCT y1, y2 FROM (SELECT y1, unnest(y2) AS y2 FROM (SELECT unnest(y1) AS y1, y2 from E3_folded_post) AS F) AS G
  SELECT * FROM E3 order by (y1::tp_interval).s NULLS FIRST, (y2::tp_interval).s NULLS FIRST;

WITH
  Ryt(country, y1, y2) AS (
    SELECT country, years, (i_left, i_right)::tp_interval
    FROM indep, tp_interval_true() AS T(i_left, i_right)
  ),
  endpoints1(country, y1, y2) AS (
   SELECT
      country, 
      int_array_sort(int_array_agg(ARRAY[(y1::tp_interval).s, (y1::tp_interval).e])), 
      int_array_sort(int_array_agg(ARRAY[(y2::tp_interval).s, (y2::tp_interval).e]))
   FROM (SELECT country, y1, y2 FROM Ryt UNION SELECT country, y2, y1 FROM Ryt) AS SQ
   GROUP BY country),
  normalized_Ryt_folded(country, y1, y2) AS (
   SELECT 
     Ryt.country,
     tp_interval_normalized_array((Ryt.y1::tp_interval).s,(Ryt.y1::tp_interval).e,NA.y1), 
     tp_interval_normalized_array((Ryt.y2::tp_interval).s,(Ryt.y2::tp_interval).e,NA.y2)
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
  tp_interval_normalized_array(L,R,
     tp_interval_normal_array(array_sort(ARRAY[2001] || int_array_agg(ARRAY[L, R+1]) OVER w), 
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
  SELECT * FROM E3 order by (y1::tp_interval).s NULLS FIRST, (y2::tp_interval).s NULLS FIRST;
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
    tp_interval_set_diff(ARRAY[(1,1),(2,2),(3,3)]::tp_interval[],
                        ARRAY[(4,4),(5,5),(6,6)]::tp_interval[],
                        ARRAY[(2,2),(6,6)]::tp_interval[],
                        ARRAY[(1,1),(5,5),(6,6)]::tp_interval[]) AS F)) AS R) AS R
                           
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
    SELECT MIN((years::tp_interval).s) FROM indep WHERE country = 'Slovakia'
  ),
  Q4(country) AS (
    SELECT DISTINCT country
    FROM indep, M
    WHERE (years::tp_interval).s < y0
  )
  SELECT * FROM Q4;

-- Q5
-- SELECT country, count(year) AS years
-- FROM indep
-- WHERE 1900 <= year AND year < 2000

WITH
  M(country, years) AS (
    SELECT country, tp_interval_intersect(years, (1900, 2000)::tp_interval) 
    FROM indep WHERE tp_interval_overlaps(years, (1900, 2000)::tp_interval)
  ),
  Q5(country, years) AS (
    SELECT country, SUM((years::tp_interval).e - (years::tp_interval).s)
    FROM M
    GROUP BY country
  )
  SELECT * FROM Q5;

-- Q6
-- SELECT year, count(country) AS numofc
-- FROM indep
-- GROUP BY year

WITH
  normalizing(min_interval) AS (
    SELECT DISTINCT min_interval FROM 
      (SELECT s, e, unnest(agg_normalized_intervals2(s, e) OVER ())
       FROM (SELECT (years::tp_interval).s, (years::tp_interval).e FROM indep 
             UNION SELECT s, e FROM tp_interval_true() AS T(s, e)) AS T) AS R(s, e, min_interval)
  ),
  normalized(country, years) AS (
    SELECT country, min_interval FROM indep I NATURAL JOIN normalizing N
    WHERE tp_interval_overlaps(min_interval, I.years)
  )
  SELECT years, COUNT(country) FROM normalized GROUP BY years

/************************************************************/

CREATE OR REPLACE FUNCTION tp_interval_true() RETURNS SETOF tp_interval LANGUAGE SQL AS $BODY$ 
  SELECT (NULL, NULL)::tp_interval;
$BODY$

CREATE OR REPLACE FUNCTION tp_interval_overlaps(IN tp_interval, IN tp_interval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  -- half-open interval semantics
  SELECT COALESCE($1.s, $2.e - 1, 0) < COALESCE($2.e, $1.s + 1, 1) AND 
         COALESCE($2.s, $1.e - 1, 0) < COALESCE($1.e, $2.s + 1, 1);
$BODY$

CREATE OR REPLACE FUNCTION tp_interval_intersect(IN tp_interval, IN tp_interval) RETURNS tp_interval LANGUAGE SQL AS $BODY$ 
  -- half-open interval semantics
  -- requires: intervals must overlap
  SELECT GREATEST(COALESCE($1.s, $2.s)), LEAST(COALESCE($1.e, $2.e));
$BODY$

CREATE OR REPLACE FUNCTION tp_interval_contains_left(IN INT, IN tp_interval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  -- half-open interval semantics
  SELECT ($1 IS NULL AND $2.s IS NULL) OR
         ($1 IS NOT NULL AND $1 >= COALESCE($2.s, $1) AND $1 < COALESCE($2.e, $1));
$BODY$

CREATE OR REPLACE FUNCTION tp_interval_contains_right(IN INT, IN tp_interval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  -- half-open interval semantics
  SELECT ($1 IS NULL AND $2.e IS NULL) OR
         ($1 IS NOT NULL AND $1 >= COALESCE($2.s, $1) AND $1 < COALESCE($2.e, $1));
$BODY$

CREATE OR REPLACE FUNCTION tp_interval_set_diff(IN tp_interval[], IN tp_interval[]) RETURNS tp_interval[] LANGUAGE SQL AS $$
  -- half-open interval semantics
  -- requires: interval arrays must be normalized
  SELECT ARRAY(
               SELECT $1[i]
                  FROM generate_series(array_lower($1,1), array_upper($1,1)) AS i
               EXCEPT
               SELECT $2[j]
                  FROM generate_series(array_lower($2,1), array_upper($2,1)) AS j
              );
$$;

CREATE OR REPLACE FUNCTION tp_interval_set_diff(IN tp_interval[], IN tp_interval[], IN tp_interval[], IN tp_interval[]) RETURNS TABLE(I1 tp_interval[], I2 tp_interval[]) LANGUAGE SQL AS $$
  -- half-open interval semantics
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
  SELECT A_C::tp_interval[], $2 FROM DIFF1
  UNION 
  SELECT tp_interval_set_diff($1, A_C), tp_interval_set_diff($2,$4) FROM DIFF1;
$$;

SELECT array_lower('{}'::int[],1),array_upper('{}'::int[],1)

SELECT * FROM
  (VALUES
  (true,tp_interval_overlaps((1,2),    (2,3))),             -- t overlaps((1, 2), (2,3))
  (true,tp_interval_overlaps((1,2),    (2,NULL))),          -- t overlaps((1, 2), (2,ifty)) 
  (true,tp_interval_overlaps((1,2),    (NULL,3))),          -- t overlaps((1, 2), (-ifty,3)) 
  (true,tp_interval_overlaps((1,2),    (NULL,NULL))),       -- t overlaps((1, 2), (-ifty,ifty)) 
  (true,tp_interval_overlaps((1,NULL), (2,3))),             -- t overlaps((1, ifty), (2,3)) 
  (true,tp_interval_overlaps((1,NULL), (2,NULL))),          -- t overlaps((1, ifty), (2,ifty)) 
  (true,tp_interval_overlaps((1,NULL), (NULL,3))),          -- t overlaps((1, ifty), (-ifty,3)) 
  (true,tp_interval_overlaps((1,NULL), (NULL,NULL))),       -- t overlaps((1, ifty), (-ifty,ifty)) 
  (true,tp_interval_overlaps((NULL,2), (2,3))),             -- t overlaps((-ifty, 2), (2,3)) 
  (true,tp_interval_overlaps((NULL,2), (2,NULL))),          -- t overlaps((-ifty, 2), (2,ifty)) 
  (true,tp_interval_overlaps((NULL,2), (NULL,3))),          -- t overlaps((-ifty, 2), (-ifty,3)) 
  (true,tp_interval_overlaps((NULL,2), (NULL,NULL))),       -- t overlaps((-ifty, 2), (-ifty,ifty)) 
  (true,tp_interval_overlaps((NULL,NULL), (2,3))),          -- t overlaps((-ifty, ifty), (2,3)) 
  (true,tp_interval_overlaps((NULL,NULL), (2,NULL))),       -- t overlaps((-ifty, ifty), (2,ifty)) 
  (true,tp_interval_overlaps((NULL,NULL), (NULL,3))),       -- t overlaps((-ifty, ifty), (-ifty,3)) 
  (true,tp_interval_overlaps((NULL,NULL), (NULL,NULL))),    -- t overlaps((-ifty, ifty), (-ifty,ifty)) 

  (false,tp_interval_overlaps((1,2),   (4,6))),             -- f overlaps((1, 2), (4,6))
  (false,tp_interval_overlaps((1,2),   (4,NULL))),          -- f overlaps((1, 2), (4,ifty)) 
  (true,tp_interval_overlaps((1,2),    (NULL,6))),          -- t overlaps((1, 2), (-ifty,6)) 
  (true,tp_interval_overlaps((1,2),    (NULL,NULL))),       -- t overlaps((1, 2), (-ifty,ifty)) 
  (true,tp_interval_overlaps((1,NULL), (4,6))),             -- t overlaps((1, ifty), (4,6)) 
  (true,tp_interval_overlaps((1,NULL), (4,NULL))),          -- t overlaps((1, ifty), (4,ifty)) 
  (true,tp_interval_overlaps((1,NULL), (NULL,6))),          -- t overlaps((1, ifty), (-ifty,6)) 
  (true,tp_interval_overlaps((1,NULL), (NULL,NULL))),       -- t overlaps((1, ifty), (-ifty,ifty)) 
  (false,tp_interval_overlaps((NULL,2),(4,6))),             -- f overlaps((-ifty, 2), (4,6)) 
  (false,tp_interval_overlaps((NULL,2),(4,NULL))),          -- f overlaps((-ifty, 2), (4,ifty)) 
  (true,tp_interval_overlaps((NULL,2), (NULL,6))),          -- t overlaps((-ifty, 2), (-ifty,6)) 
  (true,tp_interval_overlaps((NULL,2), (NULL,NULL))),       -- t overlaps((-ifty, 2), (-ifty,ifty)) 
  (true,tp_interval_overlaps((NULL,NULL), (4,6))),          -- t overlaps((-ifty, ifty), (4,6)) 
  (true,tp_interval_overlaps((NULL,NULL), (4,NULL))),       -- t overlaps((-ifty, ifty), (4,ifty)) 
  (true,tp_interval_overlaps((NULL,NULL), (NULL,6))),       -- t overlaps((-ifty, ifty), (-ifty,6)) 
  (true,tp_interval_overlaps((NULL,NULL), (NULL,NULL)))     -- t overlaps((-ifty, ifty), (-ifty,ifty)) 
  ) AS V(expected, actual)
WHERE expected <> actual;


SELECT * FROM
  (VALUES
  (true,tp_interval_contains_left(2,     (2,4))),
  (true,tp_interval_contains_left(3,     (2,4))),
  (true,tp_interval_contains_left(4,     (2,4))),
  (false,tp_interval_contains_left(1,    (2,4))),
  (false,tp_interval_contains_left(5,    (2,4))),
  (false,tp_interval_contains_left(NULL, (2,4))),
  (true,tp_interval_contains_left(2,     (NULL,4))),
  (true,tp_interval_contains_left(3,     (NULL,4))),
  (true,tp_interval_contains_left(4,     (NULL,4))),
  (true,tp_interval_contains_left(1,    (NULL,4))),
  (false,tp_interval_contains_left(5,    (NULL,4))),
  (true,tp_interval_contains_left(NULL, (NULL,4))),
  (true,tp_interval_contains_left(2,     (NULL,NULL))),
  (true,tp_interval_contains_left(3,     (NULL,NULL))),
  (true,tp_interval_contains_left(4,     (NULL,NULL))),
  (true,tp_interval_contains_left(1,    (NULL,NULL))),
  (true,tp_interval_contains_left(5,    (NULL,NULL))),
  (true,tp_interval_contains_left(NULL, (NULL,NULL))),
  (true,tp_interval_contains_right(2,     (2,4))),
  (true,tp_interval_contains_right(3,     (2,4))),
  (true,tp_interval_contains_right(4,     (2,4))),
  (false,tp_interval_contains_right(1,    (2,4))),
  (false,tp_interval_contains_right(5,    (2,4))),
  (false,tp_interval_contains_right(NULL, (2,4))),
  (true,tp_interval_contains_right(2,     (2,NULL))),
  (true,tp_interval_contains_right(3,     (2,NULL))),
  (true,tp_interval_contains_right(4,     (2,NULL))),
  (false,tp_interval_contains_right(1,    (2,NULL))),
  (true,tp_interval_contains_right(5,    (2,NULL))),
  (true,tp_interval_contains_right(NULL, (2,NULL))),
  (true,tp_interval_contains_right(2,     (NULL,NULL))),
  (true,tp_interval_contains_right(3,     (NULL,NULL))),
  (true,tp_interval_contains_right(4,     (NULL,NULL))),
  (true,tp_interval_contains_right(1,    (NULL,NULL))),
  (true,tp_interval_contains_right(5,    (NULL,NULL))),
  (true,tp_interval_contains_right(NULL, (NULL,NULL)))
  ) AS V(expected, actual)
WHERE expected <> actual;

-- 20 tuples
select unnest(ARRAY[1,2,3,4,5]), unnest(ARRAY['A','B','C','D'])

-- 5 tuples
select unnest(ARRAY[1,2,3,4,5]), unnest(ARRAY['A','B','C','D','E'])
select 
  NOT 'abc' LIKE 'a_c' true and -5 = -6 and false,
  true and false = false or true,
  (true and false) = (false or true)

-- IMPORTANT NOTE: see about encoding intervals as half-open and collecting endpoints into a single array without
-- the need to add neighborhoods of points, namely, l-1 and r+1; the set of endpoints this way easily splits a given
-- interval.