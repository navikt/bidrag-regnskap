CREATE TABLE IF NOT EXISTS konteringer
(
    kontering_id         integer NOT NULL GENERATED BY DEFAULT AS IDENTITY (INCREMENT 1 START 1 MINVALUE 1),
    oppdragsperiode_id   integer NOT NULL,
    transaksjonskode     text    NOT NULL,
    overforingsperiode   text    NOT NULL,
    overforingstidspunkt timestamp,
    type                 text    NOT NULL,
    justering            text,
    gebyr_rolle          text,
    sendt_i_palopsfil    boolean NOT NULL,

    CONSTRAINT kontering_pkey PRIMARY KEY (kontering_id),
    CONSTRAINT kontering_oppdragsperiode_fkey FOREIGN KEY (oppdragsperiode_id)
        REFERENCES oppdragsperioder (oppdragsperiode_id) MATCH SIMPLE
);