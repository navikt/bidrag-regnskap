ALTER TABLE oppdrag
ALTER COLUMN mottaker_ident SET NOT NULL;

ALTER TABLE oppdragsperioder
DROP COLUMN mottaker_ident;
