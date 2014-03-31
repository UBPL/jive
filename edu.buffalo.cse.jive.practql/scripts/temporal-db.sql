/* Input: 
 *   SELECT DISTINCT env, t 
 *   FROM Bindings 
 *   WHERE env < 'env 14' AND 2000 <= t AND t < 7000;
 * 
 * Concrete:
 *   SELECT DISTINCT env, time_s, time_e 
 *   FROM Normalize(mbr, t, Bindings) 
 *   WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000;
 *
 * Without normalization/distinct, runs in 55ms and produces 5662 tuples (Bindings has ~550K tuples)
 *
 *  SELECT env, time_s, time_e
 *  FROM Bindings 
 *  WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000;
 */

/************************************************************/
/* 1a. CTE Based Queries                                    */
/*     - closed intervals                                   */
/*     - multiple passes (4+) for interval endpoints        */
/*     - normalizing set as interval set                    */
/************************************************************/

-- cost: 7.3M, 94K tuples (93552), 660sec--700sec
-- IMin and NIMin explicitly materialized
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  left_pts(env, T) AS (
      SELECT DISTINCT env, 2000 FROM vbindings 
    UNION 
      SELECT env, time_s FROM vbindings
    UNION 
      SELECT env, time_e + 1 FROM vbindings
  ),
  right_pts(env, T) AS (
      SELECT DISTINCT env, 6999 FROM vbindings 
    UNION 
      SELECT env, time_e FROM vbindings
    UNION 
      SELECT env, time_s - 1 FROM vbindings
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
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS 
 WHERE 
   B.time_s <= BNS.R AND BNS.L <= B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 269K/414K, 94K tuples (93552), 2.15sec/3sec
-- IMin explicitly materialized with NIMin as correlated subquery
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  left_pts(env, T) AS (
      SELECT DISTINCT env, 2000 FROM vbindings 
    UNION 
      SELECT env, time_s FROM vbindings
    UNION 
      SELECT env, time_e + 1 FROM vbindings
  ),
  right_pts(env, T) AS (
      SELECT DISTINCT env, 6999 FROM vbindings 
    UNION 
      SELECT env, time_e FROM vbindings
    UNION 
      SELECT env, time_s - 1 FROM vbindings
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

-- cost: 268K/412K, 94K tuples (93552), 2.15sec/3sec
-- IMin and NIMin as subqueries
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  left_pts(env, T) AS (
      SELECT DISTINCT env, 2000 FROM vbindings 
    UNION 
      SELECT env, time_s FROM vbindings
    UNION 
      SELECT env, time_e + 1 FROM vbindings
  ),
  right_pts(env, T) AS (
      SELECT DISTINCT env, 6999 FROM vbindings 
    UNION 
      SELECT env, time_e FROM vbindings
    UNION 
      SELECT env, time_s - 1 FROM vbindings
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
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
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
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
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
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
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
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
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
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
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
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
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


/************************************************************/
/* 1d. CTE Based Queries                                    */
/*     - half-open intervals                                */
/*     - single pass for interval endpoints                 */
/*     - normalizing set as endpoint set                    */
/************************************************************/

-- cost: 7.6K, 57K tuples (57463), 10.3sec/10.7sec
-- IMin and NIMin explicitly materialized
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings
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

-- cost: 7.3K, 57K tuples (57463), 810ms/1.35sec
-- IMin explicitly materialized with NIMin as correlated subquery
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings
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

-- cost: 7.3K, 57K tuples (57463), 800ms/1.35sec
-- IMin and NIMin as subqueries
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings
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

-- cost: 6.3K, 57K tuples (57463), 280ms/820ms
-- IMin explicitly materialized using window functions
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings
  ),
  Imin(env, L, R) AS (
    SELECT env, segment[1] AS L, segment[2] AS R
    FROM (SELECT env, array_agg(T) OVER (PARTITION BY env ORDER BY T ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS segment
          FROM endpoints) AS IMin
    WHERE array_upper(segment, 1) = 2
  )
 SELECT
    env, GREATEST(BNS.L, time_s) as i_left, LEAST(BNS.R, time_e) as i_right
 FROM
    vbindings B NATURAL JOIN IMin BNS
 WHERE 
   B.time_s < BNS.R AND BNS.L < B.time_e
 ORDER BY env, i_left, i_right;

-- cost: 6.3K, 57K tuples (57463), 310ms/850ms
-- IMin as a subquery using window functions
WITH 
  vbindings(env, time_s, time_e) AS (
    SELECT env, time_s, time_e
    FROM Bindings WHERE env < 'env 14' AND 2000 <= time_s AND time_e < 7000
  ),
  endpoints(env, T) AS (
    SELECT DISTINCT env, unnest(ARRAY[2000, 7000, time_s, time_e]) FROM vbindings
  )
 SELECT
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


