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
/* 2c. View Based Queries                                   */
/*     - half-open intervals                                */
/*     - multiple passes (2+) for interval endpoints        */
/*     - normalizing set as endpoint set                    */
/************************************************************/

CREATE VIEW temp_endpoints(env, T) AS 
      SELECT DISTINCT env, 2000 FROM temp_vbindings 
    UNION 
      SELECT DISTINCT env, 7000 FROM temp_vbindings 
    UNION 
      SELECT env, time_s FROM temp_vbindings
    UNION 
      SELECT env, time_e FROM temp_vbindings;

CREATE VIEW temp_NotImin_ep(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM temp_endpoints L 
    INNER JOIN temp_endpoints I ON L.env = I.env AND L.T < I.T 
    INNER JOIN temp_endpoints R ON L.env = R.env AND I.T < R.T;

CREATE VIEW temp_Imin_ep(env, L, R) AS 
      SELECT L.env, L.T, R.T 
      FROM temp_endpoints L 
      INNER JOIN temp_endpoints R ON L.env = R.env AND L.T < R.T
    EXCEPT 
      SELECT env, L, R  FROM temp_NotImin_ep;

CREATE VIEW temp_Imin_ep_sq(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM temp_endpoints L 
    INNER JOIN temp_endpoints R ON L.env = R.env AND L.T < R.T
    WHERE 
      NOT EXISTS(SELECT * FROM temp_endpoints I WHERE L.env = I.env AND L.T < I.T AND I.T < R.T);

-- cost: 2M, 57K tuples (57463), 60.1sec/61.5sec
-- IMin and NIMin as distinct views
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_IMin_ep BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 90K/81K, 57K tuples (57463), 12.4sec/12.9sec
-- IMin as a view with NIMin as subquery
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_IMin_ep_sq BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;


/************************************************************/
/* 2d. View Based Queries                                   */
/*     - half-open intervals                                */
/*     - single pass for interval endpoints                 */
/*     - normalizing set as endpoint set                    */
/************************************************************/

  CREATE VIEW temp_endpoints_sp(env, T) AS
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM temp_vbindings;

  CREATE VIEW temp_NotImin_sp(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM temp_endpoints_sp L 
    INNER JOIN temp_endpoints_sp I ON L.env = I.env AND L.T < I.T 
    INNER JOIN temp_endpoints_sp R ON L.env = R.env AND I.T < R.T;

  CREATE VIEW temp_Imin_sp(env, L, R) AS 
      SELECT L.env, L.T, R.T 
      FROM temp_endpoints_sp L 
      INNER JOIN temp_endpoints_sp R ON L.env = R.env AND L.T < R.T
    EXCEPT 
      SELECT env, L, R  FROM temp_NotImin_sp;

  CREATE VIEW temp_Imin_sp_sq(env, L, R) AS 
    SELECT L.env, L.T, R.T 
    FROM temp_endpoints_sp L 
    INNER JOIN temp_endpoints_sp R ON L.env = R.env AND L.T < R.T
    WHERE 
      NOT EXISTS(SELECT * FROM temp_endpoints_sp I WHERE L.env = I.env AND L.T < I.T AND I.T < R.T);

  CREATE VIEW temp_Imin_sp_w(env, L, R) AS 
    SELECT env, segment[1] AS L, segment[2] AS R
    FROM (SELECT env, array_agg(T) OVER (PARTITION BY env ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
          FROM temp_endpoints_sp) AS IMin
    WHERE array_upper(segment, 1) = 2;

-- cost: 36K, 57K tuples (57463), 9.2sec/9.7sec
-- IMin and NIMin as distinct views
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_IMin_sp BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 24K, 57K tuples (57463), 6.4sec/7.1sec
-- IMin as a view with NIMin as subquery
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_IMin_sp_sq BNS 
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 6K, 57K tuples (57463), 740ms/800ms
-- IMin as a view with window function
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    temp_vbindings B NATURAL JOIN temp_Imin_sp_w BNS
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;


