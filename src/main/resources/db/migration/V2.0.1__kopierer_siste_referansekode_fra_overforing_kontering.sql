UPDATE konteringer
SET siste_referansekode = overforing_konteringer.referansekode
FROM konteringer k
JOIN overforing_konteringer on k.kontering_id = overforing_konteringer.kontering_id