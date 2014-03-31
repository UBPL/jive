/** 
 * ************************************************************
 *  REQUIRES: 
 * 
 *    1) graph(edge1, edge2) relation/view                    
 * 
 *  PROVIDES: 
 * 
 *    1) bottom-up evaluation of the same generation query
 *       for the graph relation on a particular input value, 
 *       optimized statically using magic sets; the bottom-up
 *       evaluation is semi-naive and implements the pseudo 
 *       algorithm of REF
 * ************************************************************
 */

-- all IDB predicates are augmented with an INT field to account for the round
--
-- statically rewritten program using magic sets
--
-- magic_fb(P) :- graph(P,X), magic_bf(X).
-- magic_bf(P) :- graph(P,Y), magic_fb(Y).
-- magic_bf(P) :- P is param1.
--
-- sg_bf(X,Y) :- graph(XP,X), graph(YP,Y), magic_bf(X), sg_fb(YP,XP).
-- sg_fb(X,Y) :- graph(XP,X), graph(YP,Y), magic_fb(Y), sg_bf(YP,XP).
-- sg_bf(X,X) :- magic_bf(X).
-- sg_fb(X,X) :- magic_fb(X).
--
select * from compiled_sg(120)

-- select count(*) from graph
CREATE OR REPLACE FUNCTION compiled_sg(IN X_param INT) RETURNS TABLE(xx integer, yy integer) AS
$BODY$
DECLARE
  c INT;
  r INT;
  rc INT;
  -- X_param INT;
BEGIN
  -- user's parameter that binds to X
  -- X_param := 10;
  
  -- materialized adorned magic rules
  CREATE TEMP TABLE rule_magic_bf ON COMMIT DROP AS (SELECT X_param AS P, 0 AS round);
  CREATE TEMP TABLE rule_magic_fb (P INT, round INT) ON COMMIT DROP;

  -- materialized adorned rewritten rules
  CREATE TEMP TABLE rule_sg_bf (X INT, Y INT, round INT) ON COMMIT DROP;
  CREATE TEMP TABLE rule_sg_fb (X INT, Y INT, round INT) ON COMMIT DROP;

  -- materialized answers
  CREATE TEMP TABLE answer (X INT, Y INT) ON COMMIT DROP;

  -- helper view for magic_bf rule delta evaluation
  CREATE TEMP VIEW eval_magic_bf(edge1, round) AS
    SELECT g.edge1, m.round
    FROM rule_magic_fb m INNER JOIN graph g ON m.P = g.edge2;

  -- helper view for magic_fb rule delta evaluation
  CREATE TEMP VIEW eval_magic_fb(edge1, round) AS
    SELECT g.edge1, m.round
    FROM rule_magic_bf m INNER JOIN graph g ON m.P = g.edge2;

  -- helper view for sg_bf rule evaluation
  CREATE TEMP VIEW eval_sg_bf(edge1, edge2, round) AS
    SELECT g1.edge2, g2.edge2, GREATEST(m.round, sg.round)
    FROM rule_magic_bf m 
    INNER JOIN graph g1 ON g1.edge2 = m.P
    INNER JOIN rule_sg_fb sg ON sg.X = g1.edge1
    INNER JOIN graph g2 ON sg.Y = g2.edge1
    UNION SELECT P, P, round FROM rule_magic_bf;

  -- helper view for sg_fb rule evaluation
  CREATE TEMP VIEW eval_sg_fb(edge1, edge2, round) AS
    SELECT g1.edge2, g2.edge2, GREATEST(m.round, sg.round)
    FROM rule_magic_fb m 
    INNER JOIN graph g1 ON g1.edge2 = m.P
    INNER JOIN rule_sg_bf sg ON sg.X = g1.edge1
    INNER JOIN graph g2 ON sg.Y = g2.edge1
    UNION SELECT P, P, round FROM rule_magic_fb;

  -- current round
  r := 0;
  WHILE (r = 0 OR c > 0) LOOP
    c := 0;
    r := r + 1;

    -- magic_bf delta for current round (does not duplicate tuples from previous rounds)
    INSERT INTO rule_magic_bf
      SELECT edge1, r FROM eval_magic_bf WHERE round < r EXCEPT SELECT P, r FROM rule_magic_bf;
    GET DIAGNOSTICS rc = ROW_COUNT;
    c := c + rc;

    -- magic_fb delta for current round (does not duplicate tuples from previous rounds)
    INSERT INTO rule_magic_fb 
      SELECT edge1, r FROM eval_magic_fb WHERE round < r EXCEPT SELECT P, r FROM rule_magic_fb;
    GET DIAGNOSTICS rc = ROW_COUNT;
    c := c + rc;
    
    -- sg_bf delta for current round (does not duplicate tuples from previous rounds)
    INSERT INTO rule_sg_bf
      SELECT edge1, edge2, r FROM eval_sg_bf WHERE round < r EXCEPT SELECT X, Y, r FROM rule_sg_bf;
    GET DIAGNOSTICS rc = ROW_COUNT;
    c := c + rc;
    
    -- sg_fb delta for current round (does not duplicate tuples from previous rounds)
    INSERT INTO rule_sg_fb
      SELECT edge1, edge2, r FROM eval_sg_fb WHERE round < r EXCEPT SELECT X, Y, r FROM rule_sg_fb;
    GET DIAGNOSTICS rc = ROW_COUNT;
    c := c + rc;
    
    -- update answers
    INSERT INTO answer SELECT X, Y FROM rule_sg_bf WHERE c > 0 AND round = r AND X = X_param;
  END LOOP;
  RETURN QUERY
    SELECT X, Y FROM answer;
END; $BODY$ LANGUAGE plpgsql;

