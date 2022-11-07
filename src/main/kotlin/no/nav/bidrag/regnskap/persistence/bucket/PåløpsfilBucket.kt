package no.nav.bidrag.regnskap.persistence.bucket

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.threeten.bp.Duration

@Component
class PåløpsfilBucket(
  @Value("\${BUCKET_NAME}") private val bucketNavn: String
) {

  private val retrySetting = RetrySettings.newBuilder().setTotalTimeout(Duration.ofMillis(3000)).build()
  private val storage = StorageOptions.newBuilder().setRetrySettings(retrySetting).build().service

  fun hentBucket(): Bucket {
    return storage.get(bucketNavn) ?: throw RuntimeException("Fant ikke bøtte ved navn $bucketNavn")
  }

  fun lagreFil(filnavn: String, fil: ByteArray) {
    storage.create(lagBlobinfo(filnavn), fil)
  }

  fun lagBlobinfo(filnavn: String): BlobInfo {
    return BlobInfo.newBuilder(bucketNavn, filnavn)
      .setContentType("text/plain")
      .build()
  }
}