package no.nav.bidrag.regnskap.persistence.bucket

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.WriteChannel
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import no.nav.bidrag.regnskap.util.ByteArrayOutputStreamTilByteBuffer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.threeten.bp.Duration
import java.io.InputStream
import java.nio.channels.Channels

@Component
class GcpFilBucket(
  @Value("\${BUCKET_NAME}") private val bucketNavn: String
) {

  private val retrySetting = RetrySettings.newBuilder().setTotalTimeout(Duration.ofMillis(3000)).build()
  private val storage = StorageOptions.newBuilder().setRetrySettings(retrySetting).build().service

  fun lagreFil(filnavn: String, byteArrayStream: ByteArrayOutputStreamTilByteBuffer) {
    hentWriteChannel(filnavn).use { it.write(byteArrayStream.toByteBuffer()) }
  }

  fun finnesFil(filnavn: String): Boolean {
    return storage.get(lagBlobinfo(filnavn).blobId) != null
  }

  fun hentFil(filnavn: String): InputStream {
    val reader = storage.reader(lagBlobinfo(filnavn).blobId)
    return Channels.newInputStream(reader)
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