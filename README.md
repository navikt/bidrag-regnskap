# Bidrag-regnskap

[![continuous integration](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml)
[![release bidrag-regnskap](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml)

Bidrag-regnskap er en applikasjon for å opprette og sende konteringer til Skatteetaten slik at
fakturaer kan sendes ut for bidragssaker. Dette kan både være løpende betalinger eller engangsbeløp
i form av gebyrer.

### Oppdrag

Bidrag-regnskap lytter på hendelser fra Bidrag-vedtak og lagrer hendelsene som et oppdrag.
Et oppdrag er definert unikt av stønadstype, kravhaverIdent, skyldnerIdent og ekstern referanse.
Oppdraget består av alle verdier som er felles for alle perioder oppdraget inneholder.
Disse perioden er definert som oppdragsperioder. Et oppdrag kan ha mange oppdragsperioder.
</br>Oppdrag består av følgende felter:

| Navn                  | Navn i databasen        | Beskrivelse                                                                                                                                                                                                                                                                                                              |
|-----------------------|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OppdragId             | oppdrag_id              | Primærnøkkel. ID for oppdraget. Autogeneres av postgres ved lagring av oppdraget.                                                                                                                                                                                                                                        |
| Stønadtype            | stonad_type             | Hva slags type oppdrag dette er. Er en av følgende fra enum: [StonadType.kt](https://github.com/navikt/bidrag-behandling-felles/blob/main/src/main/kotlin/no/nav/bidrag/behandling/felles/enums/StonadType.kt)                                                                                                           |
| VedtakType            | vedtak_type             | Viser til om vedtaket er opprettet manuelt eller automatisk. Er en av følgende fra enum: [EngangsbelopType.kt](https://github.com/navikt/bidrag-behandling-felles/blob/main/src/main/kotlin/no/nav/bidrag/behandling/felles/enums/EngangsbelopType.kt)                                                                   |
| KravhaverIdent        | kravhaver_ident         | Ident på kravhaver av oppdraget. I de fleste tilfeller er dette barnet.                                                                                                                                                                                                                                                  |
| SkyldnerIdent         | skyldner_ident          | Ident på skyldner av oppdraget. Dette er ofte BP i saken.                                                                                                                                                                                                                                                                |
| EksternReferanse      | ekstern_referanse       | Fritekst felt som ofte benyttes i utenlandssaker.                                                                                                                                                                                                                                                                        |
| UtsattTilDato         | utsatt_til_dato         | Saksbehandler kan ved opprettelse av en sak sette en dato som betalingen skal utsettes til. Dette kan kun gjøres ved nye vedtak. Dette vil forhindre at konteringer oversendes før utsattTil datoen er passert.                                                                                                          |
| EndretTidspunkt       | endret_tidspunkt        | Timestamp for når oppdraget sist var endret. Timestampet settes og brukes av schedlock for å identifisere om det har vært endringer på oppdraget mens locken var aktiv.                                                                                                                                                  |
| EngangsbeløpId        | engangsbelop_id         | Referer til id'en til engangsbeløp sendt fra bidrag-vedtak. Denne eksisterer kun for engangsbeløp og er det som definerer et engangsbeløp unikt. Det er gjort på denne måten siden den vanlige unike definisjon med stønadstype, kravhaverIdent, skyldnerIdent og ekstern referanse ikke nødvendigvis er unik for gebyr. |
| Oppdragsperioder      | -                       | OneToMany mapping til alle oppdragsperiodene knyttet til oppdraget.                                                                                                                                                                                                                                                      |

### Oppdragsperiode

En oppdragsperiode inneholder alle verdier knyttet til en periode. Perioden har en periode_fra og en
periode_til verdi som definerer tidsrommet en periode strekker seg utover.
Periode_til kan også være satt til `null`. I disse tilfellen har ikke perioden et satt
sluttidspunkt.
Engangsbeløp som i utgangspunktet ikke er periodisert vil her bli opprettet med en periode som kun
varer i 1 måned.
Dette er gjort for tilfredsstille ELINs krav om periode for alle beløp som overføres.
Oppdragsperioden kan kun være knyttet til et oppdrag.
</br>Oppdragsperiode består av følgende felter:

| Navn              | Navn i databasen   | Beskrivelse                                                                                                                                                                                                                                      |
|-------------------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OppdragsperiodeId | oppdragsperiode_id | Primærnøkkel. ID for oppdragsperioder. Autogeneres av postgres ved lagring av oppdragsperioden.                                                                                                                                                  |
| Oppdrag           | oppdrag            | Referanse til oppdraget oppdragsperioden tilhører.                                                                                                                                                                                               |
| SakId             | sak_id             | Iden til saken oppdragsperioden er knyttet til.                                                                                                                                                                                                  |
| VedtakId          | vedtak_id          | Iden til vedtaket oppdragsperioden er knyttet til.                                                                                                                                                                                               |
| GjelderIdent      | gjelder_ident      | Identen til den oppdragsperioden gjelder. Det gjøres et oppslag mot bidrag-sak på saksId for å sjekke om det finnes en BM på saken. Om det finnes settes gjelderIdent til BM, ellers settes gjelderIdent til dummynummer: `22222222226`          |
| MottakerIdent     | mottaker_ident     | Identen til mottaker av beløpet. Dette vil tilsvare RM. I enkelte tilfeller, slik som ved gebyr, så vil mottakerIdent settes til NAVs aktørnummer `80000345435`.                                                                                 |
| Beløp             | belop              | Beløpet oppdragsperioden har. Tallet er et desimaltall.                                                                                                                                                                                          |
| Valuta            | valuta             | Valutakode på tre bokstaver, eks `NOK`.                                                                                                                                                                                                          |
| PeriodeFra        | periode_fra        | Dato for starten på perioden. Datoen skal alltid være 1. dag i måned og er inklusiv i perioden, dvs fra og med periodeFra datoen.                                                                                                                |
| PeriodeTil        | periode_til        | Dato for slutten av perioden. Datoen skal alltid være 1. dag i måned om den er satt. Datoen kan også være null og da løper perioden helt til den blir stoppet. Datoen er ekslusiv i perioden, dvs til og _IKKE_ med periodeTil datoen.           |
| Vedtaksdato       | vedtaksdato        | Dato vedtaket ble fattet. For engangsbeløp er det denne datoen som definerer hvilken måned beløpet gjelder for.                                                                                                                                  |
| OpprettetAv       | opprettet_av       | Iden til saksbehandler som opprettet vedtaket.                                                                                                                                                                                                   |
| DelytelsesId      | delytelses_id      | Unik kode som representerer et løpende kontinuerlig oppdrag                                                                                                                                                                                      |
| AktivTil          | aktiv_til          | Feltet får satt en periode, e.g. `2023-01-01`, som representerer hvilken måned oppdragsperioden er aktiv til og ikke med. Dette kan bli satt til en annen dato enn aktivTil ved oppdateringer som går tilbake i tid tidligere enn aktivTil dato. |
| Konteringer       | -                  | OneToMany mapping til alle konteringer knyttet til oppdragsperiode.                                                                                                                                                                              |

### Kontering
En kontering er en representasjon av en måned i en oppdragsperiode. Konteringen er knyttet til en overføringsperiode som sier hvilken måned konteringen gjelder for.
</br>Kontering består av følgende felter:

| Navn                 | Navn i databasen     | Beskrivelse                                                                                                                                                                                                                                                                                                                  |
|----------------------|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KonteringId          | kontering_id         | Primærnøkkel. ID for kontering. Autogeneres av postgres ved lagring av konteringen.                                                                                                                                                                                                                                          |
| Oppdragsperiode      | oppdragsperiode_id   | Referanse til oppdragsperioden konteringen tilhører.                                                                                                                                                                                                                                                                         |
| Transaksjonskode     | transaksjonskode     | Transaksjonskoden er ELINs "oversetting" av de forskjellige stønadstypene. Til forskjell fra stønadstype så kan transaksjonskoder også være korrigerende. Se [Transaksjonskode](src/main/kotlin/no/nav/bidrag/regnskap/dto/enumer/Transaksjonskode.kt) for oversikt over transaksjonskoder og tilhørende korrigerende koder. |
| Overføringsperiode   | overforingsperiode   | Måneden konteringen gjelder for. Representert som YearMonth e.g. `2023-01`                                                                                                                                                                                                                                                   |
| Overføringstidspunkt | overforingstidspunkt | Tidspunktet konteringen ble overført til Skatteetaten. Denne er null frem til konteringen er overført.                                                                                                                                                                                                                       |
| Type                 | type                 | Feltet er (dårlig) navngitt for samsvare med slik Skatt ønsket KravAPIet. Type definerer om det er en NY eller en ENDRING av oppdraget. Det er kun førte kontering i første oppdragsperiode som skal være NY, resterende skal være ENDRING.                                                                                  |
| SøknadType           | soknad_type          | Settes til `IN` om vedtaket er `AUTOMATISK_INDEKSREGULERING`, `FABM` om vedtaket er `GEBYR_MOTTAKER`, `FABP` om vedtaket er `GEBYR_SKYLDNER`. Om ingen av disse svarer til vedtaktypen benyttes `EN`                                                                                                                         |
| SendtIPåløpsfil      | sendt_i_palopsfil    | Boolean verdi på om konteringen er sendt i påløpsfil eller ikke. Se [Skedulerte kjøringer](#skedulerte-kjøringer) for med informasjon.                                                                                                                                                                                           |
| OverføringKontering  | -                    | OneToMany mapping til alle overførte konteringer knyttet til denne konteringer.                                                                                                                                                                                                                                              |
 

### Overføring konteringer
Overføring kontering er en oversikt over alle overføringen gjort for en kontering til Skatteetaten. 
I de fleste tilfeller vil en kontering ha en overføring kontering. Om overføringen av en kontering feiler vil det derimot opprettes en overføring kontering som inneholder informasjon om feilmeldingen på hvorfor overføringen feilet.
</br>Overføring konteringer består av følgende felter:

| Navn          | Navn i databasen | Beskrivelse                                                                                                                                                                                                                                                                                                                      |
|---------------|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OverføringId  | overforing_id    | Primærnøkkel. ID for overføring kontering. Autogeneres av postgres ved lagring av overføringen.                                                                                                                                                                                                                                  |
| Kontering     | kontering_id     | Referanse til konteringen overføring konteringen tilhører.                                                                                                                                                                                                                                                                       |
| Referansekode | refernasekode    | Ved en vellykket overføring vil det returneres en UUID. Denne lagres som referansekode og kan brukes til å slå opp i ELIN for å hente ut informasjon om prosesseringen av konteringen. Denne vil være unik for hver oversending, så ikke nødvendigvis unik for hver kontering da en oversending kan inneholde flere konteringer. |                                                                                                 |
| Feilmelding   | feilmelding      | Om overføringen feiler vil feilmeldingen lagres her.                                                                                                                                                                                                                                                                             |
| Tidspunkt     | tidspunkt        | Tidspunktet overføringen ble gjennomført.                                                                                                                                                                                                                                                                                        |
| Kanal         | kanal            | Hvordan konteringen ble overført. Per nå kan dette kun være via REST-endepunkt eller Påløpsfil. Se [Skedulerte kjøringer](#skedulerte-kjøringer) eller [Integrasjoner](#integrasjoner).                                                                                                                                              |

### Påløptabell
Påløpstabellen inneholder informasjon om planlagte og gjennomført generering og overføring av påløpsfil til ELIN. Se [Skedulerte kjøringer](#skedulerte-kjøringer) for mer informasjon.
</br>Påløp består av følgende felter:

| Navn              | Navn i databasen   | Beskrivelse                                                                            |
|-------------------|--------------------|----------------------------------------------------------------------------------------|
| PåløpId           | palop_id           | Primærnøkkel. ID for overføring påløp. Autogeneres av postgres ved lagring av påløpet. |
| Kjøredato         | kjoredato          | Datoen påløpet skal kjøre på.                                                          |
| FullførtTidspunkt | fullfort_tidspunkt | Tidspunktet påløpet var ferdig overført på.                                            |
| ForPeriode        | for_periode        | Perioden påløpet er for. Representert som en YearMonth e.g. `2022-01`                  |

### Driftsavviktabell
Driftsavvik er en hendelse bidrag-regnskap selv oppretter for å hindre uønsket overføring av konteringer. 
Dette gir også bidrag-regnskap muligheten til å manuelt stoppe alle overføringer.
Se [Skedulerte kjøringer](#skedulerte-kjøringer) for mer informasjon.
</br>Driftsavvik består av følgende felter:

| Navn          | Navn i databasen | Beskrivelse                                                                                        |
|---------------|------------------|----------------------------------------------------------------------------------------------------|
| driftsavvikId | driftsavvik_id   | Primærnøkkel. ID for overføring driftsavvik. Autogeneres av postgres ved lagring av driftsavviket. |
| PåløpId       | palop_id         | Referanse til påløpet driftsavviket er knyttet til.                                                |
| TidspunktFra  | tidspunkt_fra    | Tidspunktet driftsavviket ble opprettet.                                                           |
| TidspunktTil  | tidspunkt_til    | Tidspunktet driftsavviket gjelder til.                                                             |
| OpprettetAv   | opprettet_av     | Hvem/Hva som opprettet driftsavviket. Dette er ofte den Automatiske påløpskjøringen.               |
| Årsak         | arsak            | Grunnen til at driftsavviket ble opprettet.                                                        |

## Skedulerte kjøringer
Bidrag-regnskap har flere skedulerte kjøringer. Tidspunktet disse kjører på finnes som cron-uttrykk i [application.yaml](src/main/resources/application.yaml).

### Oversending av krav
Ved opprettelse og oppdatering av oppdrag vil det forsøkes å overføre alle nye konteringer. 
Om dette ikke er mulig, enten på grunn av vedlikeholdsmodus, driftsaavik eller andre grunner, vil ikke oversendelse skje automatisk.
Det er derfor opprettet en jobb som hvert 10. minutt forsøker å overføre alle ikke overførte konteringer. Denne finnes i [SendKravScheduler.kt](src/main/kotlin/no/nav/bidrag/regnskap/hendelse/schedule/krav/SendKravScheduler.kt).

### Påløp
En gang per måned skal det opprettes en påløpsfil som overføres via filsluse til ELIN. Denne filen mellomlagres i en GCP-bucket før den overføres. Dette gjøres for enklere sporbarhet og feilsøking. 
Påløpsfilen er en XML fil som følger et strengt (og litt merkelig) format bestemt av Skatteetaten. 
Det sjekkes 5 minutter over hver time om det finnes en påløpskjøring som skal gjennomføres. 
Genereringen av påløpsfil starter kun for påløpets måned om kjøredato for påløpet er passert. 
Se [PåløpskjøringScheduler.kt](src/main/kotlin/no/nav/bidrag/regnskap/hendelse/schedule/påløp/PåløpskjøringScheduler.kt).

### Avstemming
Hver dag kl 01:00 starter generering av to avstemmingsfiler. Begge xml filene er navngitt med en fast prefix avstdet_D og avstsum_D, som etterfølges av dagens dato på yyMMdd format. 
Den første filen, avstdet_D inneholder alle oversendte konteringer for gjeldende dag. En linje i filen representerer en kontering med tilhørende verdier.
Den andre filen, avstsum er en summering over hvor mange av hver transaksjonskode som ble oversendt, samt totale beløpet alle de var på.
Se [AvstemmingsfilerScheduler.kt](src/main/kotlin/no/nav/bidrag/regnskap/hendelse/schedule/avstemning/AvstemmingsfilerScheduler.kt).

### Vedlikeholdsmodus
Vedlikeholdsmodus er en funksjon som sørger for at KravAPIet blir stengt for videre oversending av konteringer. Denne kan slå av og på ved å kalle et endepunkt i ELIN. 
Vedlikeholdsmodus blir automatisk påslått ved opprettelse av påløpsfil. Den blir derimot ikke automatisk slått av. 
Dette er fordi vi ikke har kontroll på når ELIN er ferdig med å prosessere påløpsfilen.
Se [VedlikeholdsmodusController.kt](src/main/kotlin/no/nav/bidrag/regnskap/controller/VedlikeholdsmodusController.kt).

### Driftsavvik
Driftsavvik er bidrag-regnskap sin egen måte å kontrollere oversending av konteringer. 
Denne benyttes til å hindre at nye konteringer oversendes i tilfeller hvor vi ønsker å ha bedre kontroll på kommunikasjonen mot ELIN.
Dette kan forekomme ved feks. generering av påløpsfil, eller ved planlagt vedlikehold. 
Se [DriftsavvikController.kt](src/main/kotlin/no/nav/bidrag/regnskap/controller/DriftsavvikController.kt).

## Integrasjoner

### Maskinporten
For kommunikasjon med ELIN benyttes maskinporten for validering. Det hentes et JWT-token fra maskinporten som sendes i header til ELIN for validering.
Tokenet har en varighet til 120 sekunder og blir derfor cached for å unngå unødvendig mange kall mot maskinporten.
Se [MaskinportenClient.kt](src/main/kotlin/no/nav/bidrag/regnskap/maskinporten/MaskinportenClient.kt).

### Skatt
Bidrag-regnskap kaller ELINs KravAPI for å sende over konteringer og endre status på vedlikeholdsmodus. Se [SkattConsumer.kt](src/main/kotlin/no/nav/bidrag/regnskap/consumer/SkattConsumer.kt).

### Bidrag-sak
Bidrag-regnskap har en integrasjon mot bidrag-sak for å hente ut ident til BM i saken. Se [SakConsumer.kt](src/main/kotlin/no/nav/bidrag/regnskap/consumer/SakConsumer.kt).

## Lokal utvikling

Start opp applikasjonen ved å
kjøre [BidragRegnskapLocal.kt](src/test/kotlin/no/nav/bidrag/regnskap/BidragRegnskapLocal.kt).
Dette starter applikasjonen med profil `local` og henter miljøvariabler for Q1 miljøet fra
filen [application-local.yaml](src/test/resources/application-local.yaml).

Her mangler det noen miljøvariabler som ikke bør committes til Git (Miljøvariabler for
passord/secret osv).<br/>
Når du starter applikasjon må derfor følgende miljøvariabl(er) settes:

```bash
-DAZURE_APP_CLIENT_SECRET=<secret>
-DAZURE_APP_CLIENT_ID=<id>
```

Disse kan hentes ved å kjøre kan hentes ved å kjøre

```bash
kubectl exec --tty deployment/bidrag-regnskap-feature -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET
```

For å hente ut gyldig maskinporten JWT-Token må maskinporten.privateKey legges inn i
application-local.yaml. Denne hemmeligheten kan hentes med:

```bash
kubectl exec --tty deployment/bidrag-regnskap-feature -- printenv | grep -e MASKINPORTEN_CLIENT_JWK
```

Merk: For å få client_jwk til å tolkes riktig i application-local-yaml må alle gåseøyne (") escapes
slik (\") og hele nøkkelen må omringes med et sett med gåseøyne.

### Lokal database

Dette blir opprettet når du kjører på root mappen

```bash
docker-compose up -d
```

Ved neste oppstart av applikasjonen vil flyway kjøre på den lokale databasen.
For å koble seg til databasen gjelder følgende:

```bash
jdbc:postgresql://localhost:5432/default_database
```

Brukernavn og passord:

```bash
user: cloudsqliamuser
password: admin 
```

### Kjør lokalt med kafka

Start kafka lokalt i en docker container samtidig som databasen

Bruk `kafkacat` til å sende meldinger til kafka topic.

```bash
docker run -it --rm --network=host confluentinc/cp-kafkacat kafkacat -b 0.0.0.0:9092 -t bidrag.vedtak-feature -P
```

Lim inn gyldig melding fra bidrag.vedtak topicen og deretter trykk Enter.
Da vil meldingen bli sendt til topic bidrag.vedtak-feature

### Opprette påløpsfil lokalt

Ved generering av påløpsfil blir filen streamet til en GCP Bucket før den overføres til en filsluse.
For å kunne gjøre dette lokalt så må en key-fil legges som en environment variablen i IntelliJ
configen for applikasjonen med navn GOOGLE_APPLICATION_CREDENTIALS og value må være absolutt pathen
til key-filen.

Key-fil kan
opprettes [her.](https://console.cloud.google.com/iam-admin/serviceaccounts/details/107405300865899647398/keys?project=bidrag-dev-45a9&supportedpurview=project)

Det kan hende du får noen utfordringer med å logge inn med din personlige bruker. Da må config
settes til din bruker og kjøre samme kommando igjen.
Om dette ikke fungerer må application_default_credentials.json tømmes. Denne finnes under %appdata%
-> roaming -> gcloud på windows.

### Maskinporten

For å kunne koble seg til maskinporten lokalt må maskinportens privateKey legges inn i
application-local.yaml.
Denne variablen kan hentes med:

```bash
 kubectl exec --tty deployment/bidrag-regnskap-feature -- printenv | grep -e MASKINPORTEN_CLIENT_JWK
```

NB: JWK-tokenet må omringes med tødler og alle eksisterende tødler må escapes slik \"

### JWT-Token

JWT-Token kan hentes ved hjelp at skriptet
her: [hentJwtToken](https://github.com/navikt/bidrag-dev/blob/main/scripts/hentJwtToken.sh).

### Live reload

Med `spring-boot-devtools` har Spring støtte for live-reload av applikasjon. Dette betyr i praksis
at Spring vil automatisk restarte applikasjonen når en fil endres. Du vil derfor slippe å restarte
applikasjonen hver gang du gjør endringer. Dette er forklart
i [dokumentasjonen](https://docs.spring.io/spring-boot/docs/1.5.16.RELEASE/reference/html/using-boot-devtools.html#using-boot-devtools-restart)
.
For at dette skal fungere må det gjøres noe endringer i Intellij instillingene slik at Intellij
automatisk re-bygger filene som er endret:

* Gå til `Preference -> Compiler -> check "Build project automatically"`
* Gå
  til `Preference -> Advanced settings -> check "Allow auto-make to start even if developed application is currently running"`