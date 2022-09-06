# Bidrag-template-spring

[![continuous integration](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/navikt/bidrag-regnskap/actions/workflows/ci.yaml)
[![release bidrag-regnskap](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-regnskap/actions/workflows/release.yaml)

## Beskrivelse
Bidrag-Regnskap er en applikasjon for å opprette og sende kvoteringer til Skatteetaten slik at fakturaer kan sendes ut.

## Kjøre applikasjonen lokalt

Start opp applikasjonen ved å kjøre [BidragRegnskapLocal.kt](src/test/kotlin/no/nav/bidrag/regnskap/BidragRegnskapLocal.kt).
Dette starter applikasjonen med profil `local` og henter miljøvariabler for Q1 miljøet fra filen [application-local.yaml](src/test/resources/application-local.yaml).

Her mangler det noen miljøvariabler som ikke bør committes til Git (Miljøvariabler for passord/secret osv).<br/>
Når du starter applikasjon må derfor følgende miljøvariabl(er) settes:
```bash
-DAZURE_APP_CLIENT_SECRET=<secret>
-DAZURE_APP_CLIENT_ID=<id>
```
Disse kan hentes ved å kjøre kan hentes ved å kjøre 
```bash
kubectl exec --tty deployment/bidrag-regnskap-feature -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET
```

### JWT-Token
JWT-Token kan hentes ved hjelp at skriptet her: [hentJwtToken](https://github.com/navikt/bidrag-dev/blob/main/scripts/hentJwtToken.sh).

### Live reload
Med `spring-boot-devtools` har Spring støtte for live-reload av applikasjon. Dette betyr i praksis at Spring vil automatisk restarte applikasjonen når en fil endres. Du vil derfor slippe å restarte applikasjonen hver gang du gjør endringer. Dette er forklart i [dokumentasjonen](https://docs.spring.io/spring-boot/docs/1.5.16.RELEASE/reference/html/using-boot-devtools.html#using-boot-devtools-restart).
For at dette skal fungere må det gjøres noe endringer i Intellij instillingene slik at Intellij automatisk re-bygger filene som er endret:

* Gå til `Preference -> Compiler -> check "Build project automatically"`
* Gå til `Preference -> Advanced settings -> check "Allow auto-make to start even if developed application is currently running"`