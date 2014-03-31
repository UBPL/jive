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
/* 1c. CTE Based Queries                                    */
/*     - half-open intervals                                */
/*     - multiple passes (2+) for interval endpoints        */
/*     - normalizing set as endpoint set                    */
/************************************************************/

-- cost: 2.5M/2.36M, 57K tuples (57463), 70sec/80sec
-- IMin and NIMin explicitly materialized
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, T) AS (
      SELECT DISTINCT env, 2000 FROM vbindings 
    UNION 
      SELECT DISTINCT env, 7000 FROM vbindings 
    UNION 
      SELECT env, time_s FROM vbindings
    UNION 
      SELECT env, time_e FROM vbindings
  ),
  NotImin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM endpoints L 
    INNER JOIN endpoints I ON L.env = I.env AND L.T < I.T 
    INNER JOIN endpoints R ON L.env = R.env AND I.T < R.T
  ),
  Imin(env, L, R) AS (
      SELECT L.env, L.T, R.T 
      FROM endpoints L 
      INNER JOIN endpoints R ON L.env = R.env AND L.T < R.T
    EXCEPT 
      SELECT env, L, R  FROM NotImin
  ) 
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 284K/442K, 57K tuples (57463), 1.1sec/1.63sec
-- IMin explicitly materialized with NIMin as correlated subquery
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, T) AS (
      SELECT DISTINCT env, 2000 FROM vbindings 
    UNION 
      SELECT DISTINCT env, 7000 FROM vbindings 
    UNION 
      SELECT env, time_s FROM vbindings
    UNION 
      SELECT env, time_e FROM vbindings
  ),
  Imin(env, L, R) AS (
    SELECT L.env, L.T, R.T 
    FROM endpoints L 
    INNER JOIN endpoints R ON L.env = R.env AND L.T < R.T
    WHERE 
      NOT EXISTS(SELECT * FROM endpoints I WHERE L.env = I.env AND L.T < I.T AND I.T < R.T)
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 260K/418K, 57K tuples (57463), 12.1sec/12.7sec
-- IMin and NIMin as subqueries
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 < time_e AND time_s < 7000
  ),
  endpoints(env, T) AS (
      SELECT DISTINCT env, 2000 FROM vbindings 
    UNION 
      SELECT DISTINCT env, 7000 FROM vbindings 
    UNION 
      SELECT env, time_s FROM vbindings
    UNION 
      SELECT env, time_e FROM vbindings
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B 
    NATURAL JOIN 
    -- IMin
    (
      SELECT L.env, L.T AS L, R.T AS R 
      FROM endpoints L 
      INNER JOIN endpoints R ON L.env = R.env AND L.T < R.T
      WHERE 
        NOT EXISTS(SELECT * FROM endpoints I WHERE L.env = I.env AND L.T < I.T AND I.T < R.T)
    ) BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;


