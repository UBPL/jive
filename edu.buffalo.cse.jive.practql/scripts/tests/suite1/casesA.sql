/** 
 * ************************************************************
 *  REQUIRES: 
 * 
 *    1) a bindings relation
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

/* Input: 
 *   SELECT DISTINCT env, t 
 *   FROM Bindings 
 *   WHERE env < 'env 14' AND 2000 <= t AND t < 7000;
 * 
 * Concrete:
 *   SELECT DISTINCT env, t_s, t_e 
 *   FROM Normalize({env}, {t}, Bindings) 
 *   WHERE env < 'env 14' AND 2000 < t_e AND t_s < 7000;
 *
 * Without normalization/distinct, runs in 55ms and produces 11K tuples (Bindings has 550K tuples)
 *
 *  SELECT env, t_s, t_e
 *  FROM Bindings 
 *  WHERE env < 'env 14' AND 2000 < t_e AND t_s < 7000;
 */

/************************************************************/
/* 1a. CTE Based Queries                                    */
/*     - closed intervals                                   */
/*     - multiple passes (4+) for interval endpoints        */
/*     - normalizing set as interval set                    */
/************************************************************/

-- cost: 7.3M, 94K tuples (93552), 660sec--700sec
-- IMin explicitly materialized
-- NIMin explicitly materialized
WITH 
  vbindings(env, t_s, t_e) AS (
    SELECT env, t_s, t_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < t_e AND t_s < 7000
  ),
  pts(env, T) AS (
    SELECT env, 2000 FROM vbindings 
    UNION SELECT env, 7000 FROM vbindings 
    UNION SELECT env, t_s FROM vbindings
    UNION SELECT env, t_e FROM vbindings
  ),
  NotImin(env, t_s, t_e) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN pts I ON L.env = I.env AND L.T < I.T 
    INNER JOIN pts R ON L.env = R.env AND I.T < R.T
  ),
  Imin(env, t_s, t_e) AS (
    SELECT L.env, L.T, R.T 
    FROM pts L INNER JOIN pts R ON L.env = R.env AND L.T < R.T
    EXCEPT SELECT env, t_s, t_e  FROM NotImin
  ) 
SELECT env, Im.t_s, Im.t_e
FROM vbindings B NATURAL JOIN IMin Im
WHERE B.t_s <= Im.t_s AND Im.t_e <= B.t_e
ORDER BY env, t_s, t_e;

-- cost: 269K/414K, 94K tuples (93552), 2.15sec/3sec
-- IMin explicitly materialized 
-- NIMin correlated subquery of IMin
WITH 
  vbindings(env, t_s, t_e) AS (
    SELECT env, t_s, t_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < t_e AND t_s < 7000
  ),
  pts(env, T) AS (
    SELECT env, 2000 FROM vbindings 
    UNION SELECT env, 7000 FROM vbindings 
    UNION SELECT env, t_s FROM vbindings
    UNION SELECT env, t_e FROM vbindings
  ),
  Imin(env, t_s, t_e) AS (
    SELECT L.env, L.T, R.T 
    FROM pts L INNER JOIN pts R ON L.env = R.env AND L.T < R.T
    WHERE NOT EXISTS(SELECT * FROM pts I WHERE L.env = I.env AND L.T < I.T AND I.T < R.T)
  )
SELECT env, Im.t_s, Im.t_e
FROM vbindings B NATURAL JOIN IMin Im
WHERE B.t_s <= Im.t_s AND Im.t_e <= B.t_e
ORDER BY env, t_s, t_e;

-- cost: 268K/412K, 94K tuples (93552), 2.15sec/3sec
-- IMin subquery 
-- NIMin correlated subquery of IMin
WITH 
  vbindings(env, t_s, t_e) AS (
    SELECT env, t_s, t_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < t_e AND t_s < 7000
  ),
  pts(env, T) AS (
    SELECT env, 2000 FROM vbindings 
    UNION SELECT env, 7000 FROM vbindings 
    UNION SELECT env, t_s FROM vbindings
    UNION SELECT env, t_e FROM vbindings
  ),
SELECT env, Im.t_s, Im.t_e
FROM vbindings B NATURAL JOIN 
    -- IMin
    (
      SELECT L.env, L.T AS t_s, R.T AS t_e 
      FROM pts L INNER JOIN pts R ON L.env = R.env AND L.T < R.T
      WHERE NOT EXISTS(SELECT * FROM left_pts I WHERE L.env = I.env AND L.T < I.T AND I.T < R.T)
    ) BNS 
WHERE B.t_s <= Im.t_s AND Im.t_e <= B.t_e
ORDER BY env, t_s, t_e;


