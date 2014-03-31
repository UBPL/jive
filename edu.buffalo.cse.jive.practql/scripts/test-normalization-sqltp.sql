-- all interval endpoints of R
DROP VIEW R_Times CASCADE;
CREATE OR REPLACE VIEW R_Times(datum, time) AS
  SELECT DISTINCT T.datum, T.time 
  FROM (SELECT datum, time_s AS time FROM R 
        UNION SELECT datum, time_e AS time FROM R) T;

-- all interval endpoints of S
DROP VIEW S_Times CASCADE;
CREATE OR REPLACE VIEW S_Times(datum, time) AS
  SELECT DISTINCT T.datum, T.time 
  FROM (SELECT datum, time_s AS time FROM S 
        UNION SELECT datum, time_e AS time FROM S) T;

-- all interval endpoints of R and S
DROP VIEW RS_Times CASCADE;
CREATE OR REPLACE VIEW RS_Times(datum, time) AS
  SELECT DISTINCT T.datum, T.time 
  FROM (SELECT datum, time FROM R_Times 
        UNION SELECT datum, time FROM S_Times) T;

-- construct Nx[R;R]
CREATE OR REPLACE VIEW NxR(datum, time_s, time_e, Itime_s, Itime_e) AS
  SELECT R.datum, R.time_s, R.time_e, TS.time, TE.time
  FROM R JOIN R_Times TS ON (R.datum = TS.datum) JOIN R_Times TE ON (R.datum = TE.datum)
  WHERE R.time_s <= TS.time AND TS.time < TE.time AND TE.time <= R.time_e AND
        NOT EXISTS(SELECT * FROM R_Times T 
                   WHERE R.datum = T.datum AND TS.time < T.time AND T.time < TE.time);

-- construct Nx[S;S]
CREATE OR REPLACE VIEW NxS(datum, time_s, time_e, Itime_s, Itime_e) AS
  SELECT S.datum, S.time_s, S.time_e, TS.time, TE.time
  FROM S JOIN S_Times TS ON (S.datum = TS.datum) JOIN S_Times TE ON (S.datum = TE.datum)
  WHERE S.time_s <= TS.time AND TS.time < TE.time AND TE.time <= S.time_e AND
        NOT EXISTS(SELECT * FROM S_Times T 
                   WHERE S.datum = T.datum AND TS.time < T.time AND T.time < TE.time);

-- construct Nx[R;R,S]
CREATE OR REPLACE VIEW NxR_RS(datum, time_s, time_e, Itime_s, Itime_e) AS
  SELECT R.datum, R.time_s, R.time_e, TS.time, TE.time
  FROM R JOIN RS_Times TS ON (R.datum = TS.datum) JOIN RS_Times TE ON (R.datum = TE.datum)
  WHERE R.time_s <= TS.time AND TS.time < TE.time AND TE.time <= R.time_e AND
        NOT EXISTS(SELECT * FROM RS_Times T 
                   WHERE R.datum = T.datum AND TS.time < T.time AND T.time < TE.time);

-- construct Nx[S;S,R]
CREATE OR REPLACE VIEW NxS_SR(datum, time_s, time_e, Itime_s, Itime_e) AS
  SELECT S.datum, S.time_s, S.time_e, TS.time, TE.time
  FROM S JOIN RS_Times TS ON (S.datum = TS.datum) JOIN RS_Times TE ON (S.datum = TE.datum)
  WHERE S.time_s <= TS.time AND TS.time < TE.time AND TE.time <= S.time_e AND
        NOT EXISTS(SELECT * FROM RS_Times T 
                   WHERE S.datum = T.datum AND TS.time < T.time AND T.time < TE.time);

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 1, 10); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 4, 6); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 1, 9); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 2, 10); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 2, 9); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 5, 12); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 10, 12); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 11, 12); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 5, 25), ('val', 20, 30); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

-- TEST: PASS
DELETE FROM R;
INSERT INTO R(datum, time_s, time_e) 
  VALUES('val', 1, 10), ('val', 10, 20), ('val', 20, 30); 
SELECT * FROM NxR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;


/* data for normalization tests */
delete from R;
insert into R(datum, time_s, time_e) values
('val 1',  1, 10), ('val 1', 15, 25), ('val 1', 35, 55),
('val 2',  4, 12), ('val 2', 17, 22), 
('val 3', 11, 15), ('val 3', 18, 21), ('val 3', 28, 46), ('val 3', 48, 53),
('val 4',  8, 18), ('val 4', 35, 41), ('val 4', 43, 48),
('val 5', 10, 42);

/* data for normalization tests */
delete from S;
insert into S(datum, time_s, time_e) values
('val 1',  3,  8), ('val 1', 12, 14), ('val 1', 20, 31),
('val 2',  2,  7), ('val 2', 10, 14), ('val 2', 20, 25),
('val 3',  5, 18), ('val 3', 30, 44), 
('val 4',  2, 38), 
('val 5',  7, 22);

SELECT * FROM NxR_RS ORDER BY datum, time_s, Itime_s, time_e, Itime_e;
SELECT * FROM NxS_SR ORDER BY datum, time_s, Itime_s, time_e, Itime_e;

SELECT * FROM 
  (SELECT datum, Itime_s, Itime_e FROM NxR_RS 
   INTERSECT
   SELECT datum, Itime_s, Itime_e FROM NxS_SR) AS T
ORDER BY datum, Itime_s, Itime_e;
