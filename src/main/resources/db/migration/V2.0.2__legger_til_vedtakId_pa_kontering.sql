ALTER TABLE konteringer
    ADD COLUMN vedtak_id integer;

WITH midlertidig_tabell AS (
    SELECT k.kontering_id, o.vedtak_id
    FROM konteringer k
             JOIN oppdragsperioder o ON k.oppdragsperiode_id = o.oppdragsperiode_id
)
UPDATE konteringer
SET vedtak_id = midlertidig_tabell.vedtak_id
FROM midlertidig_tabell
WHERE konteringer.kontering_id = midlertidig_tabell.kontering_id;

ALTER TABLE konteringer
    ALTER COLUMN vedtak_id SET NOT NULL;