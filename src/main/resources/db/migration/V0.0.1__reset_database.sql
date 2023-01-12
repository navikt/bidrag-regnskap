-- Slett alle tabeller for å bygge opp databasen på nytt
DROP TABLE IF EXISTS oppdrag CASCADE;
DROP TABLE IF EXISTS oppdragsperioder CASCADE;
DROP TABLE IF EXISTS konteringer CASCADE;
DROP TABLE IF EXISTS overforing_konteringer CASCADE;
DROP TABLE IF EXISTS palop CASCADE;
DROP TABLE IF EXISTS driftsavvik CASCADE;
DROP TABLE IF EXISTS shedlock CASCADE;
DROP SEQUENCE IF EXISTS delytelsesId_seq CASCADE;