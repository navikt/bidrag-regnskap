CREATE INDEX oppdrag_id_index ON oppdragsperioder (oppdrag_id);
CREATE INDEX oppdragsperiode_id_index ON konteringer (oppdragsperiode_id);
CREATE INDEX unike_identifikatorer_index ON oppdrag (stonad_type, kravhaver_ident, skyldner_ident, sak_id);