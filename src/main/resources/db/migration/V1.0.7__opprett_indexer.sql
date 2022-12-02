-- Driftsavvik
CREATE INDEX driftsavvik_palop_id_index ON driftsavvik (palop_id);
CREATE INDEX driftsavvik_tidspunkt_til_index ON driftsavvik (tidspunkt_til);

-- Konteringer
CREATE INDEX konteringer_overforingstidspunkt_index ON konteringer (overforingstidspunkt);

-- Oppdrag
CREATE INDEX oppdrag_stonad_type_index ON oppdrag (stonad_type);
CREATE INDEX oppdrag_kravhaver_ident_index ON oppdrag (kravhaver_ident);
CREATE INDEX oppdrag_skyldner_ident_index ON oppdrag (skyldner_ident);
CREATE INDEX oppdrag_ekstern_referanse_index ON oppdrag (ekstern_referanse);
CREATE INDEX oppdrag_engangsbelop_id_index ON oppdrag (engangsbelop_id);

-- Oppdragsperiode
CREATE INDEX aktiv_til_index ON oppdragsperioder (aktiv_til);

-- Påløp
CREATE INDEX for_periode_index ON palop (for_periode);
CREATE INDEX fullfort_tidspunkt_index ON palop (fullfort_tidspunkt);