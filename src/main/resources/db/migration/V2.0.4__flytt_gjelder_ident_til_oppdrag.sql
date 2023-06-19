ALTER TABLE oppdrag
    ADD COLUMN gjelder_ident text;

UPDATE oppdrag
SET gjelder_ident = oppdragsperioder.gjelder_ident
FROM oppdrag o
JOIN oppdragsperioder on o.oppdrag_id = oppdragsperioder.oppdrag_id;

ALTER TABLE oppdrag
    ALTER COLUMN gjelder_ident SET NOT NULL;

ALTER TABLE oppdragsperioder
    DROP COLUMN gjelder_ident;