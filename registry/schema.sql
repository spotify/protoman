DROP SCHEMA public CASCADE;

CREATE SCHEMA public;

CREATE TABLE file (
  id SERIAL PRIMARY KEY,
  path TEXT NOT NULL,
  content TEXT NOT NULL
);

CREATE TABLE package_version (
  id SERIAL PRIMARY KEY,
  package TEXT NOT NULL,
  major INTEGER NOT NULL,
  minor INTEGER NOT NULL,
  patch INTEGER NOT NULL
);

--GRANT USAGE ON SCHEMA public TO "anonymous-read";
--GRANT SELECT ON ALL TABLES IN SCHEMA public TO "anonymous-read";