package no.nav.bidrag.regnskap.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.bidrag.behandling.felles.enums.StonadType
import no.nav.bidrag.regnskap.dto.OppdragRequest
import no.nav.bidrag.regnskap.dto.Type
import no.nav.bidrag.regnskap.exception.OppdragFinnesAlleredeException
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.persistence.entity.Oppdragsperiode
import no.nav.bidrag.regnskap.utils.TestDataGenerator.genererPersonnummer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

  @MockK
  private lateinit var persistenceService: PersistenceService

  @MockK
  private lateinit var konteringService: KonteringService

  @InjectMockKs
  private lateinit var oppdragService: OppdragService

  @Nested
  inner class HentOppdrag {

    @Test
    fun `skal hente eksisterende oppdrag`() {
      val stonadType = StonadType.BIDRAG
      val skyldnerIdent = genererPersonnummer()

      every { persistenceService.hentOppdrag(any()) } returns Optional.of(
        opprettOppdrag(
          stonadType, skyldnerIdent
        )
      )

      val oppdragResponse = oppdragService.hentOppdrag(1)

      oppdragResponse.stonadType shouldBe stonadType
      oppdragResponse.skyldnerIdent shouldBe skyldnerIdent
    }

    @Test
    fun `skal kaste feil om oppdrag ikke eksisterer`() {
      every { persistenceService.hentOppdrag(any()) } returns Optional.empty()
      shouldThrow<NoSuchElementException> { oppdragService.hentOppdrag(1) }
    }
  }


  @Nested
  inner class LagreOppdrag {

    val oppdragRequest = opprettOppdragRequest()

    @BeforeEach
    fun setUp() {
      every { persistenceService.lagreOppdrag(any()) } returns 1
    }

    @Test
    fun `skal opprette oppdrag`() {

      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any(), any()
        )
      } returns Optional.empty()

      val oppdragId = oppdragService.lagreOppdrag(oppdragRequest)

      oppdragId shouldBe 1
    }

    @Test
    fun `skal ikke opprette oppdrag om det allerede eksisterer`() {
      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any(), any()
        )
      } returns Optional.of(opprettOppdrag(StonadType.BIDRAG, genererPersonnummer()))

      val exception = shouldThrow<OppdragFinnesAlleredeException> { oppdragService.lagreOppdrag(oppdragRequest) }
      exception.message shouldStartWith "Kombinasjonen av stonadType, kravhaverIdent, " + "skyldnerIdent og referanse viser til et allerede opprettet oppdrag"
    }
  }


  @Nested
  inner class OppdaterOppdrag {

    @BeforeEach
    fun setUp() {
      every { persistenceService.lagreOppdrag(any()) } returns 1
    }
    @Test
    fun `skal oppdatere oppdrag med nye data`() {
      val oppdrag = opprettOppdrag()
      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any(), any()
        )
      } returns Optional.of(oppdrag)


      val oppdragsId = oppdragService.oppdaterOppdrag(
        opprettOppdragRequest(
          periodeFra = LocalDate.now().minusMonths(4).withDayOfMonth(1),
          periodeTil = LocalDate.now().plusMonths(4).withDayOfMonth(1)
        )
      )

      oppdragsId shouldBe 1
      oppdrag.oppdragsperioder!! shouldHaveSize 1
      oppdrag.oppdragsperioder!![0].oppdrag shouldBe oppdrag
      oppdrag.oppdragsperioder!![0].aktivTil shouldBe null
      oppdrag.oppdragsperioder!![0].konteringer!! shouldHaveSize 8
      oppdrag.oppdragsperioder!![0].konteringer!![0].type shouldBe Type.NY.toString()
      oppdrag.oppdragsperioder!![0].konteringer!![4].type shouldBe Type.ENDRING.toString()
    }

    @Test
    fun `skal sette gamle oppdragsperioder til inaktive`() {
      val oppdragsperiode = Oppdragsperiode(
        oppdragsperiodeId = 123,
        sakId = 123456,
        vedtakId = 654321,
        gjelderIdent = genererPersonnummer(),
        mottakerIdent = genererPersonnummer(),
        belop = 7000,
        valuta = "NOK",
        periodeFra = LocalDate.now().minusMonths(1).withDayOfMonth(1),
        periodeTil = LocalDate.now().plusMonths(1).withDayOfMonth(1),
        vedtaksdato = LocalDate.now(),
        opprettetAv = "Saksbehandler123",
        delytelseId = "DelytelsesId"
      )

      val oppdrag = opprettOppdrag(oppdragsperioder = listOf(oppdragsperiode))

      every {
        persistenceService.hentOppdragPaUnikeIdentifikatorer(
          any(), any(), any(), any(), any()
        )
      } returns Optional.of(oppdrag)
      every { persistenceService.lagreOppdragsperiode(any()) } just runs

      oppdragService.oppdaterOppdrag(
        opprettOppdragRequest()
      )

      verify(exactly = 1) { persistenceService.lagreOppdragsperiode(oppdragsperiode) }
      oppdrag.oppdragsperioder!![0].aktivTil shouldBe null
    }
  }


  private fun opprettOppdrag(
    stonadType: StonadType = StonadType.BIDRAG,
    skyldnerIdent: String = genererPersonnummer(),
    oppdragsperioder: List<Oppdragsperiode>? = null
  ): Oppdrag {
    return Oppdrag(
      stonadType = stonadType.toString(),
      skyldnerIdent = skyldnerIdent,
      oppdragsperioder = oppdragsperioder
    )
  }

  private fun opprettOppdragRequest(
    stonadType: StonadType = StonadType.BIDRAG,
    kravhaverIdent: String = genererPersonnummer(),
    skyldnerIdent: String = genererPersonnummer(),
    sakId: Int = 123456,
    vedtakId: Int = 654321,
    gjelderIdent: String = genererPersonnummer(),
    mottakerIdent: String = genererPersonnummer(),
    belop: Int = 7500,
    valuta: String = "NOK",
    periodeFra: LocalDate = LocalDate.now().minusMonths(6).withDayOfMonth(1),
    periodeTil: LocalDate = LocalDate.now().plusMonths(6).withDayOfMonth(1),
    vedtakDato: LocalDate = LocalDate.now(),
    opprettetAv: String = "SaksbehandlerId",
    delytelseId: String = "DelytelseId"
  ): OppdragRequest {
    val oppdragRequest = OppdragRequest(
      stonadType = stonadType,
      kravhaverIdent = kravhaverIdent,
      skyldnerIdent = skyldnerIdent,
      sakId = sakId,
      vedtakId = vedtakId,
      gjelderIdent = gjelderIdent,
      mottakerIdent = mottakerIdent,
      belop = belop,
      valuta = valuta,
      periodeFra = periodeFra,
      periodeTil = periodeTil,
      vedtaksdato = vedtakDato,
      opprettetAv = opprettetAv,
      delytelseId = delytelseId,
    )
    return oppdragRequest
  }
}