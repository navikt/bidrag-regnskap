package no.nav.bidrag.regnskap.persistence.bucket

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.WriteChannel
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import no.nav.bidrag.regnskap.util.ByteArrayOutputStreamTilByteBuffer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.threeten.bp.Duration
import java.io.InputStream
import java.nio.channels.Channels

private val LOGGER = LoggerFactory.getLogger(GcpFilBucket::class.java)

@Component
class GcpFilBucket(
    @Value("\${BUCKET_NAME}") private val bucketNavn: String,
) {

    private val retrySetting = RetrySettings.newBuilder().setTotalTimeout(Duration.ofMillis(3000)).build()
    private val storage = StorageOptions.newBuilder().setRetrySettings(retrySetting).build().service

    fun lagreFil(filnavn: String, byteArrayStream: ByteArrayOutputStreamTilByteBuffer) {
        LOGGER.info("Starter overføring av fil: $filnavn til GCP-bucket: $bucketNavn...")
        hentWriteChannel(filnavn).use { it.write(byteArrayStream.toByteBuffer()) }
        LOGGER.info("Fil: $filnavn har blitt lastet opp til GCP-bucket: $bucketNavn!")
    }

    fun hentFil(filnavn: String): InputStream {
        val reader = storage.reader(lagBlobinfo(filnavn).blobId)
        return Channels.newInputStream(reader)
    }

    fun hentInfoOmFil(filnavn: String): String {
        val blob = storage.get(lagBlobinfo(filnavn).blobId)

        return if (blob == null) {
            "Fil: $filnavn finnes ikke i gcp-bucket!"
        } else {
            "Fil: $filnavn ble opprettet i gcp-bucket ${blob.createTimeOffsetDateTime} i bucket med navn: ${blob.bucket}."
        }
    }

    private fun hentWriteChannel(filnavn: String): WriteChannel {
        return storage.writer(lagBlobinfo(filnavn))
    }

    private fun lagBlobinfo(filnavn: String): BlobInfo {
        return BlobInfo.newBuilder(bucketNavn, filnavn)
            .setContentType("text/xml")
            .build()
    }
}
