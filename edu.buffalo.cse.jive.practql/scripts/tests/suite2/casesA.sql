/* Input: 
 *   SELECT DISTINCT env, t 
 *   FROM Bindings 
 *   WHERE env < 'env 14' AND 2000 <= t AND t < 7000;
 * 
 * Concrete:
 *   SELECT DISTINCT env, time_s, time_e 
 *   FROM Normalize({env}, {t}, Bindings) 
 *   WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000;
 *
 * Without normalization/distinct, runs in 55ms and produces 5657K tuples (Bindings has 550K tuples)
 *
 *  SELECT env, time_s, time_e
 *  FROM Bindings 
 *  WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000;
 */

/************************************************************/
/* 2a. View Based Queries                                   */
/*     - closed intervals                                   */
/*     - multiple passes (4+) for interval endpoints        */
/*     - normalizing set as interval set                    */
/************************************************************/
 
  CREATE OR REPLACE VIEW temp_vbindings(env, time_s, time_e) AS
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000;

  CREATE OR REPLACE VIEW temp_left_pts(env, T) AS
      SELECT DISTINCT env, 2000 FROM temp_vbindings 
    UNION 
      SELECT env, time_s FROM temp_vbindings
    UNION 
      SELECT env, time_e + 1 FROM temp_vbindings;

  CREATE OR REPLACE VIEW temp_right_pts(env, T) AS 
      SELECT DISTINCT env, 6999 FROM temp_vbindings 
    UNION 
      SELECT env, time_e FROM temp_vbindings
    UNION 
      SELECT env, time_s - 1 FROM temp_vbindings;

  CREATE OR REPLACE VIEW temp_NotImin(env, L, R) AS 
      SELECT L.env, L.T, R.T 
      FROM temp_left_pts L 
      INNER JOIN temp_left_pts I ON L.env = I.env AND L.T < I.T 
      INNER JOIN temp_right_pts R ON L.env = R.env AND I.T <= R.T
    UNION 
      SELECT L.env, L.T, R.T 
      FROM temp_left_pts L 
      INNER JOIN temp_right_pts I ON L.env = I.env AND L.T <= I.T 
      INNER JOIN temp_right_pts R ON L.env = R.env AND I.T < R.T;

  CREATE OR REPLACE VIEW temp_Imin(env, L, R) AS 
      SELECT L.env, L.T, R.T 
      FROM temp_left_pts L 
      INNER JOIN temp_right_pts R ON L.env = R.env AND L.T <= R.T
    EXCEPT 
      SELECT env, L, R  FROM temp_NotImin;

  CREATE OR REPLACE VIEW temp_Imin_sq(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM temp_left_pts L 
    INNER JOIN temp_right_pts R ON L.env = R.env AND L.T <= R.T
    WHERE 
      NOT EXISTS(SELECT * FROM temp_left_pts I WHERE L.env = I.env AND L.T < I.T AND I.T <= R.T)
      AND NOT EXISTS(SELECT * FROM temp_right_pts I WHERE L.env = I.env AND L.T <= I.T AND I.T < R.T)

-- cost: 6.74M, 94K tuples (93552), 619sec,672sec/703sec
-- IMin and NIMin as distinct views
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_IMin BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 91.4K/81.5K tuples (93552), 31.2sec/32.4sec
-- IMin as a view with NIMin as subquery
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_IMin_sq BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;


