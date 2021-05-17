CREATE TABLE category (
  id serial NOT NULL PRIMARY KEY,
  title varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);