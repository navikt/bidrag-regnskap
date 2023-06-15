ALTER TABLE konteringer
    ADD COLUMN sendt_i_palopsperiode text;

UPDATE konteringer
SET sendt_i_palopsperiode = 'MIGRERT'
WHERE sendt_i_palopsfil = true;

ALTER TABLE konteringer
    DROP COLUMN sendt_i_palopsfil