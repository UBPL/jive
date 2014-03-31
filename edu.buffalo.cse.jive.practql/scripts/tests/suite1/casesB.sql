/* Input: 
 *   SELECT DISTINCT env, t 
 *   FROM Bindings 
 *   WHERE env < 'env 14' AND 2000 <= t AND t < 7000;
 * 
 * Concrete:
 *   SELECT DISTINCT env, time_s, time_e 
 *   FROM Normalize({env}, {t}, Bindings) 
 *   WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000;
 *
 * Without normalization/distinct, runs in 55ms and produces 11K tuples (Bindings has 550K tuples)
 *
 *  SELECT env, time_s, time_e
 *  FROM Bindings 
 *  WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000;
 */

/************************************************************/
/* 1b. CTE Based Queries                                    */
/*     - closed intervals                                   */
/*     - single pass for interval endpoints                 */
/*     - normalizing set as interval set                    */
/************************************************************/

-- cost: 8.1K/8.2K, 94K tuples (93552), 67sec/73sec
-- IMin and NIMin explicitly materialized
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  left_pts(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000,time_s,time_e+1]) FROM vbindings 
  ),
  right_pts(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[6999,time_e,time_s-1]) FROM vbindings 
  ),
  NotImin(env, L, R) AS (
      SELECT L.env, L.T, R.T 
      FROM left_pts L 
      INNER JOIN left_pts I ON L.env = I.env AND L.T < I.T 
      INNER JOIN right_pts R ON L.env = R.env AND I.T <= R.T
    UNION 
      SELECT L.env, L.T, R.T 
      FROM left_pts L 
      INNER JOIN right_pts I ON L.env = I.env AND L.T <= I.T 
      INNER JOIN right_pts R ON L.env = R.env AND I.T < R.T
  ),
  Imin(env, L, R) AS (
      SELECT L.env, L.T, R.T 
      FROM left_pts L 
      INNER JOIN right_pts R ON L.env = R.env AND L.T <= R.T
    EXCEPT 
      SELECT env, L, R  FROM NotImin
  ) 
 SELECT
    env, time_s, time_e, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 7.5K/7.7K, 94K tuples (93552), 1.6sec/2.5sec
-- IMin explicitly materialized with NIMin as correlated subquery
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  left_pts(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000,time_s,time_e+1]) FROM vbindings 
  ),
  right_pts(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[6999,time_e,time_s-1]) FROM vbindings 
  ),
  Imin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM left_pts L 
    INNER JOIN right_pts R ON L.env = R.env AND L.T <= R.T
    WHERE 
      NOT EXISTS(SELECT * FROM left_pts I WHERE L.env = I.env AND L.T < I.T AND I.T <= R.T)
      AND NOT EXISTS(SELECT * FROM right_pts I WHERE L.env = I.env AND L.T <= I.T AND I.T < R.T)
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 7.5K/7.67K, 94K tuples (93552), 1.6sec/2.5sec
-- IMin and NIMin as subqueries
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  left_pts(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000,time_s,time_e+1]) FROM vbindings 
  ),
  right_pts(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[6999,time_e,time_s-1]) FROM vbindings 
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B 
    NATURAL JOIN 
    -- IMin
    (
      SELECT L.env, L.T AS L, R.T AS R 
      FROM left_pts L 
      INNER JOIN right_pts R ON L.env = R.env AND L.T <= R.T
      WHERE 
        NOT EXISTS(SELECT * FROM left_pts I WHERE L.env = I.env AND L.T < I.T AND I.T <= R.T)
        AND NOT EXISTS(SELECT * FROM right_pts I WHERE L.env = I.env AND L.T <= I.T AND I.T < R.T)
    ) BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;


