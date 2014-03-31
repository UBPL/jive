/** 
 * ************************************************************
 *  REQUIRES: 
 * 
 *    1) Source1 and Source2 relations/views, having a datum
 *       field, a t_s and a t_e fields to represent the interval
 * 
 *  PROVIDES: 
 * 
 *    1) FO implementation of SQL/TP N operator
 * ************************************************************
 */

﻿-- left interval endpoints associated with each datum of the source relations
CREATE VIEW t_s(datum, t_s) AS
  SELECT datum, t_s FROM Source1
  UNION SELECT datum, t_e + 1 FROM Source1
  UNION SELECT datum, t_s FROM Source2
  UNION SELECT datum, t_e + 1 FROM Source2;

-- right interval endpoints associated with each datum of the source relations
CREATE OR REPLACE VIEW t_e(datum, t_e) AS
  SELECT datum, t_e FROM Source1
  UNION SELECT datum, t_s - 1 FROM Source1
  UNION SELECT datum, t_e FROM Source2
  UNION SELECT datum, t_s - 1 FROM Source2;

-- non-minimal intervals associated with each datum of the source relations
CREATE OR REPLACE VIEW NImin(datum, t_s, t_e) AS
  SELECT L1.datum, L1.t_s, t_e.t_e
  FROM t_s AS L1 
  INNER JOIN t_s AS L2 ON (L1.datum = L2.datum AND L1.t_s < L2.t_s)
  INNER JOIN t_e ON (L1.datum = t_e.datum AND L2.t_s <= t_e);

-- minimal intervals associated with each datum of the source relations
CREATE OR REPLACE VIEW Imin(datum, t_s, t_e) AS
  SELECT t_s.datum, t_s.t_s, t_e.t_e 
  FROM t_s INNER JOIN t_e ON (t_s.datum = t_e.datum AND t_s.t_s <= t_e.t_e)
  LEFT JOIN NImin N ON (t_s.datum = N.datum AND t_s.t_s = N.t_s AND t_e.t_e = N.t_e)
  WHERE N.datum IS NULL;

-- Source1 relation with intervals partitioned into minimal intervals
CREATE OR REPLACE VIEW N_Source1(datum, t_s, t_e) AS
  SELECT S.datum, I.t_s, I.t_e
  FROM Source1 AS S
  INNER JOIN Imin I ON (S.datum = I.datum AND S.t_s <= I.t_s AND I.t_e <= S.t_e);
  
-- Source2 relation with intervals partitioned into minimal intervals
CREATE OR REPLACE VIEW N_Source2(datum, t_s, t_e) AS
  SELECT S.datum, I.t_s, I.t_e
  FROM Source2 AS S
  INNER JOIN Imin I ON (S.datum = I.datum AND S.t_s <= I.t_s AND I.t_e <= S.t_e);
