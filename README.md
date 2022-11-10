# Bidrag-template-spring

[![continuous integration](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml)
[![release bidrag-regnskap](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml)

## Beskrivelse

Bidrag-Regnskap er en applikasjon for å opprette og sende kvoteringer til Skatteetaten slik at
fakturaer kan sendes ut.

### Lokal utvikling

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

#### Lokal database

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

#### Kjør lokalt med kafka

Start kafka lokalt i en docker container samtidig som databasen

Bruk `kafkacat` til å sende meldinger til kafka topic.

```bash
docker run -it --rm --network=host confluentinc/cp-kafkacat kafkacat -b 0.0.0.0:9092 -t bidrag.vedtak-feature -P
```

Lim inn gyldig melding fra bidrag.vedtak topicen og deretter trykk Enter.
Da vil meldingen bli sendt til topic bidrag.vedtak-feature

#### Opprette påløpsfil lokalt

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