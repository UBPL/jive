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
/* 2b. View Based Queries                                   */
/*     - half-open intervals                                */
/*     - single pass for interval endpoints                 */
/*     - normalizing set as interval set                    */
/************************************************************/

CREATE VIEW left_pts_single(env, T) AS 
    SELECT DISTINCT env, unnest(ARRAY[2000,time_s,time_e+1]) FROM temp_vbindings;

CREATE VIEW right_pts_single(env, T) AS
    SELECT DISTINCT env, unnest(ARRAY[6999,time_e,time_s-1]) FROM temp_vbindings;

CREATE VIEW NotImin_single(env, L, R) AS 
      SELECT L.env, L.T, R.T 
      FROM left_pts_single L 
      INNER JOIN left_pts_single I ON L.env = I.env AND L.T < I.T 
      INNER JOIN right_pts_single R ON L.env = R.env AND I.T <= R.T
    UNION 
      SELECT L.env, L.T, R.T 
      FROM left_pts_single L 
      INNER JOIN right_pts_single I ON L.env = I.env AND L.T <= I.T 
      INNER JOIN right_pts_single R ON L.env = R.env AND I.T < R.T;

CREATE VIEW Imin_single(env, L, R) AS 
      SELECT L.env, L.T, R.T 
      FROM left_pts_single L 
      INNER JOIN right_pts_single R ON L.env = R.env AND L.T <= R.T
    EXCEPT 
      SELECT env, L, R  FROM NotImin_single;

CREATE VIEW Imin_single_sq(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM left_pts_single L 
    INNER JOIN right_pts_single R ON L.env = R.env AND L.T <= R.T
    WHERE 
      NOT EXISTS(SELECT * FROM left_pts_single I WHERE L.env = I.env AND L.T < I.T AND I.T <= R.T)
      AND NOT EXISTS(SELECT * FROM right_pts_single I WHERE L.env = I.env AND L.T <= I.T AND I.T < R.T);

-- cost: 54K, 94K tuples (93552), 55sec/57.3sec
-- IMin and NIMin as distinct views
 SELECT
    env, time_s, time_e, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN IMin_single BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 30K, 94K tuples (93552), 17.4sec/18.9sec
-- IMin as a view with NIMin as subquery
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN IMin_single_sq BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;


