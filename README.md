# Bidrag-regnskap

[![continuous integration](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml)
[![release bidrag-regnskap](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml)

Bidrag-regnskap er en applikasjon for å opprette og sende konteringer til Skatteetaten slik at
fakturaer kan sendes ut for bidragssaker. Dette kan både være løpende betalinger eller engangsbeløp
i form av gebyrer.

### Oppdrag

Bidrag-regnskap lytter på hendelser fra Bidrag-vedtak og lagrer hendelsene som et oppdrag.

Oppdrag kan deles i to hovedgrupper, engangsbeløp og stønader. 
Engangsbeløp består av særtilskudd, gebyr mottaker, gebyr skyldner, tilbakekreving, ettergivelse, direkte oppgjør og ettergivelse tilbakekreving.
Disse har en periode på kun 1 mnd. 

Stønader består av forskudd, bidrag, oppfostringsbidrag, 18 års bidrag, ektefellebidrag og motregning.
Disse er ofte løpende over flere mnd, og kan eksistere med en satt sluttdato eller uten kjent sluttdato.


Et oppdrag er definert unikt av stønadstype, kravhaverIdent, skyldnerIdent og sakId.
Oppdraget består av alle verdier som er felles for alle perioder oppdraget inneholder.
Disse perioden er definert som oppdragsperioder. Et oppdrag kan ha mange oppdragsperioder.

Et oppdrag inneholder følgende nevneverdige felter:

| Navn               | Beskrivelse                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Stønadtype         | Hva slags type oppdrag dette er. Er fra en av følgende enumer: [StonadType.kt](https://github.com/navikt/bidrag-domain/blob/28a5a86914ad8bdaad0eeaf884813dedeb1647a8/src/main/kotlin/no/nav/bidrag/domain/enums/StonadType.kt) [EngangsbelopType.kt](https://github.com/navikt/bidrag-domain/blob/28a5a86914ad8bdaad0eeaf884813dedeb1647a8/src/main/kotlin/no/nav/bidrag/domain/enums/EngangsbelopType.kt) |
| VedtakType         | Viser til om det var fastsettelse, krage, indeksregulering e.g som førte til vedtaket. Er en av følgende fra enum: [VedtakType.kt](https://github.com/navikt/bidrag-domain/blob/28a5a86914ad8bdaad0eeaf884813dedeb1647a8/src/main/kotlin/no/nav/bidrag/domain/enums/VedtakType.kt)                                                                                                                         |
| SakId             | Iden til saken oppdraget er knyttet til.                                                                                                                                                                                                                                                                                                                                                                   |
| KravhaverIdent     | Ident på kravhaver av oppdraget. I de fleste tilfeller er dette barnet.                                                                                                                                                                                                                                                                                                                                    |
| SkyldnerIdent      | Ident på skyldner av oppdraget. Dette er ofte BP i saken.                                                                                                                                                                                                                                                                                                                                                  |
| GjelderIdent      | Identen til den oppdragsperioden gjelder. Det gjøres et oppslag mot bidrag-sak på saksId for å sjekke om det finnes en BM på saken. Om det finnes settes gjelderIdent til BM, ellers settes gjelderIdent til dummynummer: `22222222226` |
| UtsattTilDato      | Saksbehandler kan ved opprettelse av en sak sette en dato som betalingen skal utsettes til. Dette kan kun gjøres ved nye vedtak. Dette vil forhindre at konteringer oversendes før utsattTil datoen er passert.                                                                                                                                                                                            |

### Oppdragsperiode

En oppdragsperiode inneholder alle verdier knyttet til en periode. Perioden har en periode_fra og en
periode_til verdi som definerer tidsrommet en periode strekker seg utover.
Periode_til kan også være satt til `null`. I disse tilfellen har ikke perioden et satt
sluttidspunkt.
Engangsbeløp som i utgangspunktet ikke er periodisert vil her bli opprettet med en periode som kun
varer i 1 måned.
Dette er gjort for tilfredsstille ELINs krav om periode for alle beløp som overføres.
Oppdragsperioden kan kun være knyttet til et oppdrag.

Et oppdrag inneholder følgende nevneverdige felter:

| Navn              | Beskrivelse                                                                                                                                                                                                                       |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| VedtakId          | Iden til vedtaket oppdragsperioden er knyttet til.                                                                                                                                                                                |
| Referanse         | Referanse til vedtaket som ble fattet for et engangsbeløp. Vil ikke eksistere for løpende oppdragsperioder.                                                                                                                       |
| MottakerIdent     | Identen til mottaker av beløpet. Dette vil tilsvare RM. I enkelte tilfeller, slik som ved gebyr, så vil mottakerIdent settes til NAVs aktørnummer `80000345435`.                                                                  |
| Beløp             | Beløpet oppdragsperioden har. Tallet er et desimaltall.                                                                                                                                                                           |
| Valuta            | Valutakode på tre bokstaver, eks `NOK`.                                                                                                                                                                                           |
| PeriodeFra        | Dato for starten på perioden. Datoen skal alltid være 1. dag i måned og er inklusiv i perioden, dvs fra og med periodeFra datoen.                                                                                                 |
| PeriodeTil        | Dato for slutten av perioden. Datoen skal alltid være 1. dag i måned om den er satt. Datoen kan også være null og da løper perioden helt til den blir stoppet. Datoen er ekslusiv i perioden, dvs til og _IKKE_ med periodeTil datoen. |
| Vedtaksdato       | Dato vedtaket ble fattet. For engangsbeløp er det denne datoen som definerer hvilken måned beløpet gjelder for.                                                                                                                   |
| OpprettetAv       | Iden til saksbehandler som opprettet vedtaket.                                                                                                                                                                                    |
| DelytelsesId      | Unik kode som representerer et løpende kontinuerlig oppdrag                                                                                                                                                                       |
| AktivTil          | Feltet får satt en periode, e.g. `2023-01-01`, som representerer hvilken måned oppdragsperioden er aktiv til og ikke med. Dette kan bli satt til en annen dato enn aktivTil ved oppdateringer som går tilbake i tid tidligere enn aktivTil dato. |

### Kontering
En kontering er en representasjon av en måned i en oppdragsperiode. Konteringen er knyttet til en overføringsperiode som sier hvilken måned konteringen gjelder for.
Et oppdrag inneholder følgende nevneverdige felter:

| Navn                         | Beskrivelse                                                                                                                                                                                                                                                                                                                  |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Transaksjonskode             | Transaksjonskoden er ELINs "oversetting" av de forskjellige stønadstypene. Til forskjell fra stønadstype så kan transaksjonskoder også være korrigerende. Se [Transaksjonskode](src/main/kotlin/no/nav/bidrag/regnskap/dto/enumer/Transaksjonskode.kt) for oversikt over transaksjonskoder og tilhørende korrigerende koder. |
| Overføringsperiode           | Måneden konteringen gjelder for. Representert som YearMonth e.g. `2023-01`                                                                                                                                                                                                                                                   |
| Overføringstidspunkt         | Tidspunktet konteringen ble overført til Skatteetaten. Denne er null frem til konteringen er overført.                                                                                                                                                                                                                       |
| BehandlingsstatusOkTidspunkt | Tidspunktet vi fikk en godkjenning fra ELIN om at konteringen var vellykket lest inn. Denne er null frem til konteringen er bekreftet godkjent. Dette feltet hindrer videre oversendinger av konteringer for samme oppdrag frem til bekreftet ok.                                                                            |
| Type                         | Feltet er navngitt for samsvare med KravAPIet. Type definerer om det er en NY eller en ENDRING av oppdraget. Det er kun førte kontering i første oppdragsperiode som skal være NY, resterende skal være ENDRING.                                                                                                             |
| SøknadType                   | Settes til `IN` om vedtaket er `AUTOMATISK_INDEKSREGULERING`, `FABM` om vedtaket er `GEBYR_MOTTAKER`, `FABP` om vedtaket er `GEBYR_SKYLDNER`. Om ingen av disse svarer til vedtaktypen benyttes `EN`                                                                                                                         |
| SendtIPåløpsfil              | Boolean verdi på om konteringen er sendt i påløpsfil eller ikke. Se [Skedulerte kjøringer](#skedulerte-kjøringer) for med informasjon.                                                                                                                                                                                       |
 

### Påløptabell
Påløpstabellen inneholder informasjon om planlagte og gjennomført generering og overføring av påløpsfil til ELIN. Se [Skedulerte kjøringer](#skedulerte-kjøringer) for mer informasjon.

Påløp består av følgende felter:

| Navn              | Beskrivelse                                                                                                                                                                    |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Kjøredato         | Datoen påløpet skal kjøre på.                                                                                                                                                  |
| StartetTidspunkt  | Tidspunktet påløpet ble startet. Dette eksisterer for å ikke starte påbegynte påløp på nytt i tilfeller hvor påløp tar lenger tid enn intervallet mellom skedulerte kjøringer. |
| FullførtTidspunkt | Tidspunktet påløpet var ferdig overført på.                                                                                                                                    |
| ForPeriode        | Perioden påløpet er for. Representert som en YearMonth e.g. `2022-01`                                                                                                          |

### Driftsavviktabell
Driftsavvik er en hendelse bidrag-regnskap selv oppretter for å hindre uønsket overføring av konteringer. 
Dette gir også bidrag-regnskap muligheten til å manuelt stoppe alle overføringer.
Se [Skedulerte kjøringer](#skedulerte-kjøringer) for mer informasjon.

Driftsavvik består av følgende felter:

| Navn          |  Beskrivelse                                                                                        |
|---------------|-----------------------------------------------------------------------------------------------------|
| PåløpId       |  Referanse til påløpet driftsavviket er knyttet til.                                                |
| TidspunktFra  |  Tidspunktet driftsavviket ble opprettet.                                                           |
| TidspunktTil  |  Tidspunktet driftsavviket gjelder til.                                                             |
| OpprettetAv   |  Hvem/Hva som opprettet driftsavviket. Dette er ofte den Automatiske påløpskjøringen.               |
| Årsak         |  Grunnen til at driftsavviket ble opprettet.                                                        |



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

### Bidrag-person
Bidrag-regnskap benytter seg av @SjekkForNyIdent annotasjon for å søke etter nye identer på vedtak som mottas fra bidrag-vedtak. Se: [IdentUtils.kt](src/main/kotlin/no/nav/bidrag/regnskap/util/IdentUtils.kt) for bruk.

### PDL
Bidrag-regnskap lytter på AktorV2 topic fra PDL for å lese inn endringer på identer. Se [AktørhendelseListener](src/main/kotlin/no/nav/bidrag/regnskap/hendelse/kafka/pdl/AktørhendelseListener.kt)

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