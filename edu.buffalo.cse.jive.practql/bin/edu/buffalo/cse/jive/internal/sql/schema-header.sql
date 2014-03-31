-- Jive's execution model has a timestamp YYYYMMDDHHNNSS
-- this timestamp is used when creating the database schema
-- the JDBC connection to the database must set the search patḧ:
-- SET search_path TO jiveYYYYMMDDHHNNSS,public;
-- snapshoting from the database should be almost trivial;
-- this will enable offline processing of Jive using a "jive load" command

-- NOTE: the RDBMS version of Jive's model violates encapsulation

DROP SCHEMA jiveYYYYMMDDHHNNSS CASCADE;
CREATE SCHEMA jiveYYYYMMDDHHNNSS;
ALTER DEFAULT PRIVILEGES IN SCHEMA jiveyyyymmddhhnnss GRANT ALL ON TABLES TO public;
GRANT ALL ON SCHEMA jiveyyyymmddhhnnss TO public;
SET SEARCH_PATH TO jiveYYYYMMDDHHNNSS,public;

-- 1. Create a template temporal database from which all other temporal databases will copy
--
-- CREATE DATABASE template_temporal TEMPLATE template0;
--
-- in PUBLIC: create types, functions, tables, views, etc, providing temporal support
--
-- 2. Create a temporal database from the temporal_template
--
-- CREATE DATABASE myTemporal TEMPLATE template_temporal;