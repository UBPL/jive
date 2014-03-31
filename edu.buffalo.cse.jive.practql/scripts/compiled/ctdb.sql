--DROP SCHEMA ctdb;
--CREATE SCHEMA ctdb;
--ALTER DEFAULT PRIVILEGES IN SCHEMA ctdb GRANT ALL ON TABLES TO public;
--GRANT ALL ON SCHEMA ctdb TO public;

--SET SEARCH_PATH TO ctdb,public;

/** 
 * ************************************************************
 *  MAIN AND AUXILIARY TYPES
 * ************************************************************
 */
 
DROP DOMAIN tp CASCADE;
DROP DOMAIN tp_encoded CASCADE;
DROP TYPE cinterval CASCADE;
DROP TYPE flagged_int CASCADE;
DROP TYPE partition_state CASCADE;

CREATE DOMAIN tp AS INT;
COMMENT ON DOMAIN tp IS 'Abstract time point, isomorphic to the integers. Encodes a single time instant.';

CREATE DOMAIN tp_encoded AS INT;
COMMENT ON DOMAIN tp_encoded IS 'Abstract time point, isomorphic to the integers. Encodes a half-open time interval, represented as a cinterval in the CTDB.';

CREATE TYPE cinterval AS (_left INT, _right INT);
COMMENT ON TYPE cinterval IS 'Concrete interval type encoded as a tuple consisting of left (_left) and right (_right) endpoint representing the half-open interval [_left, _right).';

CREATE TYPE flagged_int AS (_value INT, _flag BOOLEAN);
COMMENT ON TYPE flagged_int IS 'Internal type used to encode a "flagged" integer, that is, one associated with a boolean value.';

CREATE TYPE partition_state AS (_i INT[], _lnull boolean, _rnull boolean);
COMMENT ON TYPE partition_state IS 'Internal type used to encode a "flagged" integer array. The flags indicate whether a null was seen for the left/right endpoints, respectively.';

/** 
 * ************************************************************
 *  CONVENIENCE VIEWS USED FOR COMPLEMENTATION
 * ************************************************************
 */

CREATE OR REPLACE VIEW _true(t) AS  SELECT (NULL, NULL)::cinterval;
COMMENT ON VIEW _true IS 'Convenience view representing the open interval (-infty, +infty).';

CREATE OR REPLACE VIEW _ptrue(t) AS  SELECT (0, NULL)::cinterval;
COMMENT ON VIEW _ptrue IS 'Convenience view representing the half-open interval [0, +infty).';

CREATE OR REPLACE VIEW _ntrue(t) AS  SELECT (NULL, 0)::cinterval;
COMMENT ON VIEW _ntrue IS 'Convenience view representing the open interval (-infty, 0).';

/** 
 * ************************************************************
 *  BASIC INTERVAL OPERATIONS
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION has_pred(IN i cinterval, IN p INT) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($2 IS NOT NULL) AND 
         ($1._left IS NULL OR $1._left < $2);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION has_pred(cinterval, INT) IS 'RETURNS true if i has a predecessor of p: there exists a point q in i such that q < p.'; 

CREATE OR REPLACE FUNCTION has_predi(IN i cinterval, IN p INT) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($2 IS NOT NULL) AND 
         ($1._left IS NULL OR $1._left <= $2);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION has_predi(cinterval, INT) IS 'RETURNS true if i has a predecessor of p or p itself: there exists a point q in i such that q <= p.'; 

CREATE OR REPLACE FUNCTION has_succ(IN i cinterval, IN p INT) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($2 IS NOT NULL) AND 
         ($1._right IS NULL OR $1._right > $2 + 1);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION has_succ(cinterval, INT) IS 'RETURNS true if i has a successor of p: there exists a point q in i such that q > p.'; 

CREATE OR REPLACE FUNCTION has_succi(IN i cinterval, IN p INT) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($2 IS NOT NULL) AND 
         ($1._right IS NULL OR $1._right > $2);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION has_succi(cinterval, INT) IS 'RETURNS true if i has a successor of p or p itself: there exists a point q in i such that q >= p.'; 

CREATE OR REPLACE FUNCTION contains_left(IN p INT, IN i cinterval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($1 IS NULL AND $2._left IS NULL) OR
         ($1 IS NOT NULL AND COALESCE($2._left, $1) <= $1 AND $1 < COALESCE($2._right, $1+1));
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION contains_left(INT, cinterval) IS 'RETURNS true if p is -infty and i._left is -infty OR p is an integer value contained in the interval i.'; 

CREATE OR REPLACE FUNCTION contains_right(IN p INT, IN i cinterval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT ($1 IS NULL AND $2._right IS NULL) OR
         ($1 IS NOT NULL AND COALESCE($2._left, $1) <= $1 AND $1 < COALESCE($2._right, $1+1));
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION contains_right(INT, cinterval) IS 'RETURNS true if p is +infty and i._right is +infty OR p is an integer value contained in the interval i.'; 

CREATE OR REPLACE FUNCTION get_left(IN i cinterval) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT $1._left;
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION get_left(cinterval) IS 'RETURNS the left endpoint of the input interval.'; 

CREATE OR REPLACE FUNCTION get_right(IN i cinterval) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT $1._right;
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION get_right(cinterval) IS 'RETURNS the right endpoint of the input interval.'; 


CREATE OR REPLACE FUNCTION ctdb.less_lr(i1 ctdb.cinterval, i2 ctdb.cinterval)
  RETURNS boolean AS
$BODY$ 
  SELECT COALESCE($1._left, $2._right-1, -1) < COALESCE($2._right, $2._left+1, +1);
$BODY$
  LANGUAGE sql IMMUTABLE
  COST 100;
ALTER FUNCTION ctdb.less_lr(ctdb.cinterval, ctdb.cinterval)
  OWNER TO demian;
COMMENT ON FUNCTION ctdb.less_lr(ctdb.cinterval, ctdb.cinterval) IS 'RETURNS i1- < i2+.';


CREATE OR REPLACE FUNCTION ctdb.inc_l(i ctdb.cinterval)
  RETURNS ctdb.cinterval AS
$BODY$ 
  SELECT ($1._left + 1, $1._right)::ctdb.cinterval;
$BODY$
  LANGUAGE sql IMMUTABLE
  COST 100;
ALTER FUNCTION ctdb.inc_l(ctdb.cinterval)
  OWNER TO demian;
COMMENT ON FUNCTION ctdb.inc_l(ctdb.cinterval) IS 'Returns the input interval with the left endpoint incremented.';


CREATE OR REPLACE FUNCTION overlapping(IN i1 cinterval, IN i2 cinterval) RETURNS BOOLEAN LANGUAGE SQL AS $BODY$ 
  SELECT COALESCE($1._left, $2._right-1, 0) < COALESCE($2._right, $1._left+1, 1) AND 
         COALESCE($2._left, $1._right-1, 0) < COALESCE($1._right, $2._left+1, 1);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION overlapping(cinterval, cinterval) IS 'RETURNS true if i1 and i2 have at least one common instant.';

CREATE OR REPLACE FUNCTION intersection(IN i1 cinterval, IN i2 cinterval) RETURNS cinterval LANGUAGE SQL AS $BODY$ 
  SELECT GREATEST(COALESCE($1._left, $2._left)), LEAST(COALESCE($1._right, $2._right));
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION intersection(cinterval, cinterval) IS 'RETURNS the interval containing all instants common to i1 and i2. REQUIRES: input intervals must overlap.';

/** 
 * ************************************************************
 *  SET DIFFERENCE
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION normal_set_diff(IN A cinterval[], IN B cinterval[]) RETURNS SETOF cinterval LANGUAGE SQL AS $BODY$
  SELECT _left, _right FROM unnest($1) AS A(_left, _right) 
  EXCEPT SELECT _left, _right FROM unnest($2) AS B(_left, _right)
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION normal_set_diff(cinterval[], cinterval[]) IS 'RETURNS a set of distinct intervals such that each interval belongs to A but not B. REQUIRES: interval arrays must be normalized.';

CREATE OR REPLACE FUNCTION normal_set_diff_array(IN A cinterval[], IN B cinterval[]) RETURNS cinterval[] LANGUAGE SQL AS $BODY$
  SELECT ARRAY(SELECT normal_set_diff($1,$2));
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION normal_set_diff_array(cinterval[], cinterval[]) IS 'RETURNS an array of distinct intervals such that each interval belongs to A but not B. REQUIRES: interval arrays must be normalized.';

/** 
 * ************************************************************
 *  INTERVAL PROJECTION
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION project(IN I cinterval, IN P cinterval[], OUT _iout cinterval) RETURNS SETOF cinterval AS
$BODY$
DECLARE
  L INT;
  R INT;
  ptr INT;
  max_ptr INT;
  flag BOOLEAN;
BEGIN
  L := I._left;
  R := I._right;
  flag := true;
  ptr := array_lower(P,1);
  max_ptr := array_upper(P,1);
  WHILE ptr <= max_ptr AND flag LOOP
    IF contains_left(L, P[ptr]) THEN
      _iout._left := L;
      IF contains_right(R-1, P[ptr]) THEN
        _iout._right := R;
        RETURN NEXT;
        flag := false;
      ELSE
        _iout._right := P[ptr]._right;
        RETURN NEXT;
        L := _iout._right;
      END IF;
    END IF;
    ptr := ptr + 1;
  END LOOP;
  RETURN;
END; 
$BODY$ LANGUAGE plpgsql IMMUTABLE;
COMMENT ON FUNCTION project(cinterval, cinterval[]) IS 'RETURNS the projection of the interval I=[L,R) onto the partition P. REQUIRES that P is a minimal partition.';

CREATE OR REPLACE FUNCTION project_sql(IN I cinterval, IN _iin cinterval[], OUT _iout cinterval) RETURNS SETOF cinterval LANGUAGE SQL ROWS 10 AS 
$BODY$ 
  SELECT GREATEST($1._left, _left), LEAST($1._right, _right) 
  FROM unnest($2) AS F(_left, _right)
  WHERE overlapping($1, (_left, _right)::cinterval);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION project_sql(cinterval, cinterval[]) IS 'RETURNS the projection of the interval I=[L,R) onto the partition P. REQUIRES that P is a minimal partition. SQL version of the similarly named function-- the two versions exist for profiling their performance differences.';

/** 
 * ************************************************************
 *  INTERVAL AGGREGATE: PARTITION
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION array_distinct(ANYARRAY) RETURNS ANYARRAY LANGUAGE SQL AS $BODY$
  SELECT ARRAY(SELECT DISTINCT i FROM unnest($1) AS s(i) ORDER BY 1);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION array_distinct(ANYARRAY) IS 'RETURNS an array consisting of the distinct elements in the input array.';
 
CREATE OR REPLACE FUNCTION minimal_partition(IN Ps INT[]) RETURNS cinterval[] LANGUAGE SQL AS $BODY$ 
  SELECT ARRAY(
    SELECT _i
    FROM 
      (SELECT
         row_number() OVER W AS rn,
         (FIRST_VALUE(T) OVER W, LAST_VALUE(T) OVER W)::cinterval AS _i
       FROM unnest($1) AS R(T)
       WINDOW w AS (ROWS BETWEEN 1 PRECEDING AND CURRENT ROW)) AS W
     WHERE rn > 1
  );
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION minimal_partition(INT[]) IS 'RETURNS an array with the minimal partition of intervals constructed from the input array. REQUIRES an ordered array of non-null integers possibly with an initial NULL value (-infty) and a terminal NULL value (+infty).';

CREATE OR REPLACE FUNCTION _agg_partition_update(IN _state partition_state, IN _item cinterval) RETURNS partition_state LANGUAGE SQL AS $BODY$ 
  SELECT 
    CASE WHEN $2._left IS NULL AND $2._right IS NULL THEN $1._i::INT[]
          WHEN $2._right IS NULL THEN $1._i::INT[] || $2._left
          WHEN $2._left IS NULL THEN $1._i::INT[] || $2._right
          ELSE $1._i::INT[] || ARRAY[$2._left, $2._right] END,
     $1._lnull OR $2._left IS NULL,
     $1._rnull OR $2._right IS NULL
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_partition_update(partition_state, cinterval) IS 'RETURNS the updated state of the partition after processing the input interval.';

CREATE OR REPLACE FUNCTION _agg_partition_finalize(IN partition_state) RETURNS cinterval[] LANGUAGE SQL AS $BODY$ 
  SELECT
    minimal_partition(_i)
  FROM 
    (SELECT 
     CASE WHEN $1._lnull AND $1._rnull THEN NULL::INT || array_distinct($1._i) || NULL::INT
          WHEN $1._lnull THEN NULL::INT || array_distinct($1._i) 
          WHEN $1._rnull THEN array_distinct($1._i) || NULL::INT
          ELSE array_distinct($1._i) END) AS T(_i)
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_partition_finalize(partition_state) IS 'RETURNS an integer array in a form suitable for partitioning.';

CREATE AGGREGATE agg_partition(cinterval) (
  SFUNC = _agg_partition_update,
  STYPE = partition_state,
  FINALFUNC = _agg_partition_finalize,
  INITCOND = '({},false,false)'
);
COMMENT ON AGGREGATE agg_partition(cinterval) IS 'RETURNS the array of intervals corresponding to the minimal partition of the intervals.';

CREATE OR REPLACE FUNCTION interval_partition(IN A cinterval[]) RETURNS cinterval[] LANGUAGE SQL AS $BODY$ 
  SELECT agg_partition((_left, _right)::cinterval) FROM unnest($1) AS F(_left, _right);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION interval_partition(cinterval[]) IS 'RETURNS the array of intervals corresponding to the minimal partition of the intervals.';

/** 
 * ************************************************************
 *  INTERVAL AGGREGATE: MIN
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION _agg_min_update(IN _state flagged_int, IN _item cinterval) RETURNS flagged_int LANGUAGE SQL AS $BODY$ 
  SELECT CASE WHEN NOT $1._flag AND $2._left IS NOT NULL THEN (LEAST($1._value, $2._left), FALSE)::flagged_int ELSE (NULL, TRUE)::flagged_int END; 
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_min_update(flagged_int, cinterval) IS 'RETURNS the updated flagged integer encoding the minimum left endpoint.';

CREATE OR REPLACE FUNCTION _agg_min_finalize(IN _state flagged_int) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT CASE WHEN $1._flag THEN NULL ELSE $1._value END; 
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_min_finalize(flagged_int) IS 'RETURNS the integer encoding the minimum left endpoint.';

CREATE AGGREGATE agg_min(cinterval) (
  SFUNC = _agg_min_update,
  STYPE = flagged_int,
  FINALFUNC = _agg_min_finalize,
  INITCOND = '(,false)'
);
COMMENT ON AGGREGATE agg_min(cinterval) IS 'RETURNS the integer encoding the minimum left endpoint.';

CREATE OR REPLACE FUNCTION interval_min(IN A cinterval[]) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT agg_min((_left, _right)::cinterval) FROM unnest($1) AS F(_left, _right);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION interval_min(cinterval[]) IS 'RETURNS the integer encoding the minimum left endpoint across all intervals in the array.';

/** 
 * ************************************************************
 *  INTERVAL AGGREGATE: MAX
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION _agg_max_update(IN _state flagged_int, IN _item cinterval) RETURNS flagged_int LANGUAGE SQL AS $BODY$ 
  SELECT CASE WHEN NOT $1._flag AND $2._right IS NOT NULL THEN (GREATEST($1._value, $2._right), FALSE)::flagged_int ELSE (NULL, TRUE)::flagged_int END; 
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_max_update(flagged_int, cinterval) IS 'RETURNS the updated flagged integer encoding the maximum right endpoint.';

CREATE OR REPLACE FUNCTION _agg_max_finalize(IN _state flagged_int) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT CASE WHEN $1._flag THEN NULL ELSE $1._value END; 
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_max_finalize(flagged_int) IS 'RETURNS the integer encoding the maximum right endpoint.';

CREATE AGGREGATE agg_max(cinterval) (
  SFUNC = _agg_max_update,
  STYPE = flagged_int,
  FINALFUNC = _agg_max_finalize,
  INITCOND = '(,false)'
);
COMMENT ON AGGREGATE agg_max(cinterval) IS 'RETURNS the integer encoding the maximum right endpoint.';

CREATE OR REPLACE FUNCTION interval_max(IN A cinterval[]) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT agg_max((_left, _right)::cinterval) FROM unnest($1) AS F(_left, _right);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION interval_max(cinterval[]) IS 'RETURNS the integer encoding the maximum right endpoint across all intervals in the array.';

/** 
 * ************************************************************
 *  INTERVAL AGGREGATE: COUNT
 * ************************************************************
 */

CREATE OR REPLACE FUNCTION _agg_count_update(IN _state flagged_int, IN _item cinterval) RETURNS flagged_int LANGUAGE SQL AS $BODY$ 
  SELECT 
    CASE WHEN NOT $1._flag AND $2._left IS NOT NULL AND $2._right IS NOT NULL THEN 
           ($1._value + ($2._right - $2._left), FALSE)::flagged_int 
         ELSE 
           (NULL, TRUE)::flagged_int 
    END; 
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_count_update(flagged_int, cinterval) IS 'RETURNS the upated flagged integer encoding the sum of the interval durations.';

CREATE OR REPLACE FUNCTION _agg_count_finalize(IN _state flagged_int) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT CASE WHEN $1._flag THEN NULL ELSE $1._value END; 
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION _agg_count_finalize(flagged_int) IS 'RETURNS the integer encoding the sum of all interval durations.';

CREATE AGGREGATE agg_count(cinterval) (
  SFUNC = _agg_count_update,
  STYPE = flagged_int,
  FINALFUNC = _agg_count_finalize,
  INITCOND = '(0,false)'
);
COMMENT ON AGGREGATE agg_count(cinterval) IS 'RETURNS the integer encoding the sum of all interval durations.';

CREATE OR REPLACE FUNCTION interval_count(IN A cinterval[]) RETURNS INT LANGUAGE SQL AS $BODY$ 
  SELECT agg_count((_left, _right)::cinterval) FROM unnest($1) AS F(_left, _right);
$BODY$ IMMUTABLE;
COMMENT ON FUNCTION interval_count(cinterval[]) IS 'RETURNS an integer representing the sum of the durations of all intervals in the array. A NULL return value indicates infinite duration.';

/** 
 * ************************************************************
 *  EXAMPLES
 * ************************************************************
 */

-- select a null interval
--SELECT NULL::cinterval AS I;

-- select a single interval
--SELECT (1,2)::cinterval AS I;

-- select the left endpoint of the interval
--SELECT ((1,2)::cinterval)._left;

-- select the right endpoint of the interval
--SELECT ((1,2)::cinterval)._right;

-- select a constant bag of intervals
--SELECT I, (I::cinterval)._left, (I::cinterval)._right
--FROM (VALUES ((1,10)::cinterval), ((5,15)::cinterval), ((5,15)::cinterval), ((15,25)::cinterval)) AS F(I);

-- select a constant set of intervals
--SELECT DISTINCT I, (I::cinterval)._left, (I::cinterval)._right
--FROM (VALUES ((1,10)::cinterval), ((5,15)::cinterval), ((5,15)::cinterval), ((15,25)::cinterval)) AS F(I);


--SELECT * FROM
--  (VALUES
--  ('A', false, overlapping((1,     2),    (2,     3))),    -- t overlaps((1, 2), (2,3))
--  ('B', false, overlapping((1,     2),    (2,     NULL))), -- t overlaps((1, 2), (2,ifty)) 
--  ('C', true,  overlapping((1,     2),    (NULL, 3))),     -- t overlaps((1, 2), (-ifty,3)) 
--  ('D', true,  overlapping((1,     2),    (NULL, NULL))),  -- t overlaps((1, 2), (-ifty,ifty)) 
--  ('E', true,  overlapping((1,     NULL), (2,     3))),    -- t overlaps((1, ifty), (2,3)) 
--  ('F', true,  overlapping((1,     NULL), (2,     NULL))), -- t overlaps((1, ifty), (2,ifty)) 
--  ('G', true,  overlapping((1,     NULL), (NULL, 3))),     -- t overlaps((1, ifty), (-ifty,3)) 
--  ('H', true,  overlapping((1,     NULL), (NULL, NULL))),  -- t overlaps((1, ifty), (-ifty,ifty)) 
--  ('I', false, overlapping((NULL, 2),     (2,     3))),    -- t overlaps((-ifty, 2), (2,3)) 
--  ('J', false, overlapping((NULL, 2),     (2,     NULL))), -- t overlaps((-ifty, 2), (2,ifty)) 
--  ('K', true,  overlapping((NULL, 2),     (NULL, 3))),     -- t overlaps((-ifty, 2), (-ifty,3)) 
--  ('L', true,  overlapping((NULL, 2),     (NULL, NULL))),  -- t overlaps((-ifty, 2), (-ifty,ifty)) 
--  ('M', true,  overlapping((NULL, NULL),  (2,     3))),    -- t overlaps((-ifty, ifty), (2,3)) 
--  ('N', true,  overlapping((NULL, NULL),  (2,     NULL))), -- t overlaps((-ifty, ifty), (2,ifty)) 
--  ('O', true,  overlapping((NULL, NULL),  (NULL, 3))),     -- t overlaps((-ifty, ifty), (-ifty,3)) 
--  ('P', true,  overlapping((NULL, NULL),  (NULL, NULL))),  -- t overlaps((-ifty, ifty), (-ifty,ifty)) 
--  ('Q', false, overlapping((1,     2),    (4,    6))),     -- f overlaps((1, 2), (4,6))
--  ('R', false, overlapping((1,     2),    (4,    NULL))),  -- f overlaps((1, 2), (4,ifty)) 
--  ('S', true,  overlapping((1,     2),    (NULL, 6))),     -- t overlaps((1, 2), (-ifty,6)) 
--  ('T', true,  overlapping((1,     2),    (NULL, NULL))),  -- t overlaps((1, 2), (-ifty,ifty)) 
--  ('U', true,  overlapping((1,     NULL), (4,     6))),    -- t overlaps((1, ifty), (4,6)) 
--  ('V', true,  overlapping((1,     NULL), (4,     NULL))), -- t overlaps((1, ifty), (4,ifty)) 
--  ('W', true,  overlapping((1,     NULL), (NULL, 6))),     -- t overlaps((1, ifty), (-ifty,6)) 
--  ('X', true,  overlapping((1,     NULL), (NULL, NULL))),  -- t overlaps((1, ifty), (-ifty,ifty)) 
--  ('Y', false, overlapping((NULL, 2),     (4,    6))),     -- f overlaps((-ifty, 2), (4,6)) 
--  ('Z', false, overlapping((NULL, 2),     (4,    NULL))),  -- f overlaps((-ifty, 2), (4,ifty)) 
--  ('0', true,  overlapping((NULL, 2),     (NULL, 6))),     -- t overlaps((-ifty, 2), (-ifty,6)) 
--  ('1', true,  overlapping((NULL, 2),     (NULL, NULL))),  -- t overlaps((-ifty, 2), (-ifty,ifty)) 
--  ('2', true,  overlapping((NULL, NULL), (4,     6))),     -- t overlaps((-ifty, ifty), (4,6)) 
--  ('3', true,  overlapping((NULL, NULL), (4,     NULL))),  -- t overlaps((-ifty, ifty), (4,ifty)) 
--  ('4', true,  overlapping((NULL, NULL), (NULL, 6))),      -- t overlaps((-ifty, ifty), (-ifty,6)) 
--  ('5', true,  overlapping((NULL, NULL), (NULL, NULL)))    -- t overlaps((-ifty, ifty), (-ifty,ifty)) 
--  ) AS V(id, expected, actual)
--WHERE expected <> actual;


--SELECT * FROM
--  (VALUES
--  ('A', true,  contains_left(2,     (2,4))),
--  ('B', true,  contains_left(3,     (2,4))),
--  ('C', false, contains_left(4,     (2,4))),
--  ('D', false, contains_left(1,     (2,4))),
--  ('E', false, contains_left(5,     (2,4))),
--  ('F', false, contains_left(NULL,  (2,4))),
--  ('G', true,  contains_left(2,     (NULL,4))),
--  ('H', true,  contains_left(3,     (NULL,4))),
--  ('I', false, contains_left(4,     (NULL,4))),
--  ('J', true,  contains_left(1,     (NULL,4))),
--  ('K', false, contains_left(5,     (NULL,4))),
--  ('L', true,  contains_left(NULL,  (NULL,4))),
--  ('M', true,  contains_left(2,     (NULL,NULL))),
--  ('N', true,  contains_left(3,     (NULL,NULL))),
--  ('O', true,  contains_left(4,     (NULL,NULL))),
--  ('P', true,  contains_left(1,     (NULL,NULL))),
--  ('Q', true,  contains_left(5,     (NULL,NULL))),
--  ('R', true,  contains_left(NULL,  (NULL,NULL))),
--  
--  ('S', true,  contains_right(2,    (2,4))),
--  ('T', true,  contains_right(3,    (2,4))),
--  ('U', false, contains_right(4,    (2,4))),
--  ('V', false, contains_right(1,    (2,4))),
--  ('W', false, contains_right(5,    (2,4))),
--  ('X', false, contains_right(NULL, (2,4))),
--  ('Y', true,  contains_right(2,    (2,NULL))),
--  ('Z', true,  contains_right(3,    (2,NULL))),
--  ('0', true,  contains_right(4,    (2,NULL))),
--  ('1', false, contains_right(1,    (2,NULL))),
--  ('2', true,  contains_right(5,    (2,NULL))),
--  ('3', true,  contains_right(NULL, (2,NULL))),
--  ('4', true,  contains_right(2,    (NULL,NULL))),
--  ('5', true,  contains_right(3,    (NULL,NULL))),
--  ('6', true,  contains_right(4,    (NULL,NULL))),
--  ('7', true,  contains_right(1,    (NULL,NULL))),
--  ('8', true,  contains_right(5,    (NULL,NULL))),
--  ('9', true,  contains_right(NULL, (NULL,NULL)))
--  ) AS V(id, expected, actual)
--WHERE expected <> actual;


-- SELECT agg_partition(i) FROM (VALUES((NULL,50)::cinterval), ((10, 300)::cinterval), ((150, NULL)::cinterval)) AS A(i)
-- SELECT agg_partition(i) FROM (VALUES((-3,20)::cinterval), ((19, 100)::cinterval), ((99, 600)::cinterval), ((999, 1050)::cinterval)) AS A(i)

-- SELECT interval_partition(ARRAY(SELECT i FROM (VALUES((NULL,50)::cinterval), ((10, 300)::cinterval), ((150, NULL)::cinterval)) AS A(i)))
-- SELECT interval_partition(ARRAY(SELECT i FROM (VALUES((-3,20)::cinterval), ((19, 100)::cinterval), ((99, 600)::cinterval), ((999, 1050)::cinterval)) AS A(i)))

-- SELECT agg_min(i) FROM (VALUES((NULL,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)
-- SELECT agg_min(i) FROM (VALUES((-3,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)

-- SELECT interval_min(ARRAY(SELECT i FROM (VALUES((NULL,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)))
-- SELECT interval_min(ARRAY(SELECT i FROM (VALUES((-3,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)))

-- SELECT agg_max(i) FROM (VALUES((NULL,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)
-- SELECT agg_max(i) FROM (VALUES((-3,1)::cinterval), ((10, 100)::cinterval), ((150, 800)::cinterval)) AS A(i)

-- SELECT interval_max(ARRAY(SELECT i FROM (VALUES((NULL,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)))
-- SELECT interval_max(ARRAY(SELECT i FROM (VALUES((-3,1)::cinterval), ((10, 100)::cinterval), ((150, 800)::cinterval)) AS A(i)))

-- SELECT agg_sum(i) FROM (VALUES((NULL,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)
-- SELECT agg_sum(i) FROM (VALUES((-3,1)::cinterval), ((10, 100)::cinterval), ((150, 800)::cinterval)) AS A(i)

-- SELECT interval_sum(ARRAY(SELECT i FROM (VALUES((NULL,1)::cinterval), ((10, 100)::cinterval), ((150, NULL)::cinterval)) AS A(i)))
-- SELECT interval_sum(ARRAY(SELECT i FROM (VALUES((-3,1)::cinterval), ((10, 100)::cinterval), ((150, 800)::cinterval)) AS A(i)))
