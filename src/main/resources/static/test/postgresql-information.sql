-- show users
SELECT u.usename AS "Role name",
  CASE WHEN u.usesuper AND u.usecreatedb THEN CAST('superuser, create database' AS pg_catalog.text)
       WHEN u.usesuper THEN CAST('superuser' AS pg_catalog.text)
       WHEN u.usecreatedb THEN CAST('create database' AS pg_catalog.text)
       ELSE CAST('' AS pg_catalog.text)
  END AS "Attributes"
FROM pg_catalog.pg_user u
ORDER BY 1;

-- show databases
SELECT datname FROM pg_database;

-- show tables
SELECT
    schemaname as "Schema",
    tablename as "Name",
    tableowner as "Owner"
FROM pg_catalog.pg_tables
WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema';

-- describe tables
SELECT
    column_name as "Column",
    udt_name as "Type",
    collation_name as "Collation",
    is_nullable as "Nullable",
    column_default as "Default"
FROM information_schema.COLUMNS
WHERE TABLE_NAME = '${TABLE_NAME}';