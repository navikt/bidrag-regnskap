package no.nav.bidrag.regnskap.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.commons.util.PersonidentGenerator
import no.nav.bidrag.domene.enums.regnskap.Transaksjonskode
import no.nav.bidrag.domene.enums.regnskap.Type
import no.nav.bidrag.domene.enums.regnskap.behandlingsstatus.Batchstatus
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import no.nav.bidrag.transport.regnskap.behandlingsstatus.Feilmelding
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@ExtendWith(MockKExtension::class)
class OppslagServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var skattConsumer: SkattConsumer

    @InjectMockKs
    private lateinit var oppslagService: OppslagService

    @Nested
    inner class HentOppdrag {

        @Test
        fun `skal hente eksisterende oppdrag`() {
            val stonadType = Stønadstype.BIDRAG
            val skyldnerIdent = PersonidentGenerator.genererFødselsnummer()

            every { persistenceService.hentOppdrag(any()) } returns TestData.opprettOppdrag(
                stonadType = stonadType,
                skyldnerIdent = skyldnerIdent,
            )

            val oppdragResponse = oppslagService.hentOppdrag(1)!!

            oppdragResponse.type shouldBe stonadType.name
            oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
        }

        @Test
        fun `skal være tom om oppdrag ikke eksisterer`() {
            every { persistenceService.hentOppdrag(any()) } returns null
            oppslagService.hentOppdrag(1) shouldBe null
        }
    }

    @Nested
    inner class HentOppdragsperioder {

        @Test
        fun `Skal hente oppdragsperiode`() {
            val oppdrag = TestData.opprettOppdrag()
            val opprettetOppdragsperiodeListe = oppslagService.hentOppdragsperioderMedKonteringer(oppdrag)

            opprettetOppdragsperiodeListe shouldHaveSize oppdrag.oppdragsperioder.size
        }
    }

    @Nested
    inner class HentKonteringer {
        @Test
        fun `Skal hente alle kontering på et oppdrag`() {
            val transaksjonskode = Transaksjonskode.B1
            val overforingsperiode = YearMonth.now()

            val oppdrag = TestData.opprettOppdrag(
                oppdragsperioder = listOf(
                    TestData.opprettOppdragsperiode(
                        konteringer = listOf(
                            TestData.opprettKontering(
                                konteringId = 1,
                                transaksjonskode = transaksjonskode.toString(),
                                overforingsperiode = overforingsperiode.toString(),
                                type = Type.NY.name,
                            ),
                            TestData.opprettKontering(
                                konteringId = 2,
                                transaksjonskode = transaksjonskode.toString(),
                                overforingsperiode = overforingsperiode.plusMonths(1).toString(),
                                type = Type.ENDRING.name,
                            ),
                        ),
                    ),
                ),
            )

            val konteringResponseListe = oppslagService.hentKonteringer(oppdrag.oppdragsperioder.first())

            konteringResponseListe[0].konteringId shouldBe 1
            konteringResponseListe[1].konteringId shouldBe 2
            konteringResponseListe[0].transaksjonskode shouldBe transaksjonskode
            konteringResponseListe[1].transaksjonskode shouldBe transaksjonskode
            konteringResponseListe[0].overforingsperiode shouldBe overforingsperiode.toString()
            konteringResponseListe[1].overforingsperiode shouldBe overforingsperiode.plusMonths(1).toString()
            konteringResponseListe[0].type shouldBe Type.NY
            konteringResponseListe[1].type shouldBe Type.ENDRING
        }
    }

    @Nested
    inner class HentUtsatteVedtakForSak {

        @Test
        fun `skal opprette liste med utsatte vedtak`() {
            val now = LocalDateTime.now()
            val saksnummer = Saksnummer("123456789")
            val utsattTilDato = LocalDate.now().plusDays(1)
            val oppdrag = TestData.opprettOppdrag(
                sakId = saksnummer.verdi,
                utsattTilDato = utsattTilDato,
            )
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                oppdrag = oppdrag,
            )
            val kontering = TestData.opprettKontering(
                oppdragsperiode = oppdragsperiode,
            ).apply {
                overføringstidspunkt = now
                behandlingsstatusOkTidspunkt = now
            }
            oppdragsperiode.konteringer = listOf(kontering)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode)

            every { persistenceService.hentAlleOppdragPåSakId(saksnummer.verdi) } returns listOf(oppdrag)

            val utsatteOgFeiledeVedtak = oppslagService.hentUtsatteOgFeiledeVedtakForSak(saksnummer)

            utsatteOgFeiledeVedtak.utsatteVedtak shouldHaveSize 1
            utsatteOgFeiledeVedtak.feiledeVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.ikkeOversendteVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.utsatteVedtak.first().utsattTil shouldBe utsattTilDato
        }

        @Test
        fun `skal ha tom liste med utsatte vedtak om vedtakk ikke er utsatt lenger enn i dag`() {
            val now = LocalDateTime.now()
            val saksnummer = Saksnummer("123456789")
            val oppdrag = TestData.opprettOppdrag(
                sakId = saksnummer.verdi,
                utsattTilDato = LocalDate.now(),
            )
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                oppdrag = oppdrag,
            )
            val kontering = TestData.opprettKontering(
                oppdragsperiode = oppdragsperiode,
            ).apply {
                overføringstidspunkt = now
                behandlingsstatusOkTidspunkt = now
            }
            oppdragsperiode.konteringer = listOf(kontering)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode)

            every { persistenceService.hentAlleOppdragPåSakId(saksnummer.verdi) } returns listOf(oppdrag)

            val utsatteOgFeiledeVedtak = oppslagService.hentUtsatteOgFeiledeVedtakForSak(saksnummer)

            utsatteOgFeiledeVedtak.utsatteVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.feiledeVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.ikkeOversendteVedtak shouldHaveSize 0
        }

        @Test
        fun `skal ha liste med feilede vedtak`() {
            val now = LocalDateTime.now()
            val generertReferansekode = UUID.randomUUID().toString()
            val saksnummer = Saksnummer("123456789")
            val feilmelding = "FEILET"
            val oppdrag = TestData.opprettOppdrag(
                sakId = saksnummer.verdi,
                utsattTilDato = null,
            )
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                oppdrag = oppdrag,
            )
            val kontering = TestData.opprettKontering(
                oppdragsperiode = oppdragsperiode,
            ).apply {
                overføringstidspunkt = now
                behandlingsstatusOkTidspunkt = null
                sisteReferansekode = generertReferansekode
            }
            oppdragsperiode.konteringer = listOf(kontering)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode)

            every { persistenceService.hentAlleOppdragPåSakId(saksnummer.verdi) } returns listOf(oppdrag)
            every { skattConsumer.sjekkBehandlingsstatus(generertReferansekode) } returns ResponseEntity.ok(
                BehandlingsstatusResponse(
                    listOf(Feilmelding(null, null, null, null, null, feilmelding)),
                    Batchstatus.Failed,
                    UUID.randomUUID().toString(),
                    1,
                    1,
                    0,
                ),
            )

            val utsatteOgFeiledeVedtak = oppslagService.hentUtsatteOgFeiledeVedtakForSak(saksnummer)

            utsatteOgFeiledeVedtak.utsatteVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.feiledeVedtak shouldHaveSize 1
            utsatteOgFeiledeVedtak.ikkeOversendteVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.feiledeVedtak.first().feilmelding shouldBe feilmelding
        }

        @Test
        fun `skal ha tomme lister`() {
            val now = LocalDateTime.now()
            val generertReferansekode = UUID.randomUUID().toString()
            val saksnummer = Saksnummer("123456789")
            val oppdrag = TestData.opprettOppdrag(
                sakId = saksnummer.verdi,
                utsattTilDato = null,
            )
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                oppdrag = oppdrag,
            )
            val kontering = TestData.opprettKontering(
                oppdragsperiode = oppdragsperiode,
            ).apply {
                overføringstidspunkt = now
                behandlingsstatusOkTidspunkt = null
                sisteReferansekode = generertReferansekode
            }
            oppdragsperiode.konteringer = listOf(kontering)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode)

            every { persistenceService.hentAlleOppdragPåSakId(saksnummer.verdi) } returns listOf(oppdrag)
            every { skattConsumer.sjekkBehandlingsstatus(generertReferansekode) } returns ResponseEntity.ok(
                BehandlingsstatusResponse(
                    emptyList(),
                    Batchstatus.Done,
                    UUID.randomUUID().toString(),
                    1,
                    0,
                    1,
                ),
            )

            val utsatteOgFeiledeVedtak = oppslagService.hentUtsatteOgFeiledeVedtakForSak(saksnummer)

            utsatteOgFeiledeVedtak.utsatteVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.feiledeVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.ikkeOversendteVedtak shouldHaveSize 0
        }

        @Test
        fun `skal ha liste over ikke overførte vedtak`() {
            val generertReferansekode = UUID.randomUUID().toString()
            val saksnummer = Saksnummer("123456789")
            val vedtakId = 123
            val oppdrag = TestData.opprettOppdrag(
                sakId = saksnummer.verdi,
                utsattTilDato = null,
            )
            val oppdragsperiode = TestData.opprettOppdragsperiode(
                oppdrag = oppdrag,
                vedtakId = vedtakId,
            )
            val kontering = TestData.opprettKontering(
                oppdragsperiode = oppdragsperiode,
            ).apply {
                overføringstidspunkt = null
                behandlingsstatusOkTidspunkt = null
                sisteReferansekode = generertReferansekode
            }
            oppdragsperiode.konteringer = listOf(kontering)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode)

            every { persistenceService.hentAlleOppdragPåSakId(saksnummer.verdi) } returns listOf(oppdrag)
            every { skattConsumer.sjekkBehandlingsstatus(generertReferansekode) } returns ResponseEntity.ok(
                BehandlingsstatusResponse(
                    emptyList(),
                    Batchstatus.Done,
                    UUID.randomUUID().toString(),
                    1,
                    0,
                    1,
                ),
            )

            val utsatteOgFeiledeVedtak = oppslagService.hentUtsatteOgFeiledeVedtakForSak(saksnummer)

            utsatteOgFeiledeVedtak.utsatteVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.feiledeVedtak shouldHaveSize 0
            utsatteOgFeiledeVedtak.ikkeOversendteVedtak shouldHaveSize 1
            utsatteOgFeiledeVedtak.ikkeOversendteVedtak.first().vedtakId shouldBe vedtakId
        }
    }
}
