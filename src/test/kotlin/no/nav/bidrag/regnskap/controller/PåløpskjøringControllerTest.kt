package no.nav.bidrag.regnskap.controller

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.test.runTest
import no.nav.bidrag.regnskap.persistence.repository.OppdragsperiodeRepository
import no.nav.bidrag.regnskap.service.ManglendeKonteringerService
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import no.nav.bidrag.regnskap.utils.TestData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus

@ExtendWith(MockKExtension::class)
class PåløpskjøringControllerTest {

    @MockK(relaxed = true)
    private lateinit var påløpskjøringService: PåløpskjøringService

    @MockK(relaxed = true)
    private lateinit var manglendeKonteringerService: ManglendeKonteringerService

    @MockK(relaxed = true)
    private lateinit var oppdragsperiodeRepo: OppdragsperiodeRepository

    @InjectMockKs
    private lateinit var påløpskjøringController: PåløpskjøringController

    @Test
    fun `skal ved påløpskjøring returnere 201`() = runTest {
        val påløp = TestData.opprettPåløp(påløpId = 1, fullførtTidspunkt = null, forPeriode = "2022-01")

        every { påløpskjøringService.hentPåløp() } returns påløp

        val response = påløpskjøringController.startPåløpskjøring(false)

        response.statusCode shouldBe HttpStatus.CREATED
    }

    @Test
    fun `skal om det ikke finnes ikke kjørte påløpsperioder returnere 204`() = runTest {
        every { påløpskjøringService.hentPåløp() } returns null

        val response = påløpskjøringController.startPåløpskjøring(false)

        response.statusCode shouldBe HttpStatus.NO_CONTENT
    }
}
