package com.kara.kara_general_api.infrastructure.adapter.output.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.kara.kara_general_api.domain.port.output.ImageStoragePort
import com.kara.kara_general_api.domain.port.output.ImageVisibility
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Adaptateur GCS unique du magasin d'objets ([ImageStoragePort]) : images publiques (CDN) comme documents
 * privés (PDF de reçus servis par URL signée). Un seul client Storage, un bucket par visibilité.
 */
@Component
@Profile("!test")
class GcsImageStorageAdapter(
    private val storage: Storage,
    @Value("\${GCS_BUCKET_PUBLIC}") private val publicBucket: String,
    @Value("\${GCS_BUCKET_PRIVATE}") private val privateBucket: String,
    @Value("\${GCS_CDN_BASE_URL}") private val cdnBaseUrl: String,
) : ImageStoragePort {

    override fun upload(visibility: ImageVisibility, key: String, bytes: ByteArray, contentType: String) {
        val blobInfo =
            BlobInfo.newBuilder(BlobId.of(bucketFor(visibility), key))
                .setContentType(contentType)
                .build()
        storage.create(blobInfo, bytes)
    }

    override fun exists(visibility: ImageVisibility, key: String): Boolean =
        storage.get(BlobId.of(bucketFor(visibility), key)) != null

    override fun delete(visibility: ImageVisibility, key: String) {
        storage.delete(BlobId.of(bucketFor(visibility), key))
    }

    override fun signedUrl(key: String, ttl: Duration): String {
        val blobInfo = BlobInfo.newBuilder(BlobId.of(privateBucket, key)).build()
        return storage.signUrl(
            blobInfo,
            ttl.toMinutes(),
            TimeUnit.MINUTES,
            Storage.SignUrlOption.withV4Signature(),
        ).toString()
    }

    override fun publicUrl(key: String): String = "${cdnBaseUrl.trimEnd('/')}/$key"

    private fun bucketFor(visibility: ImageVisibility): String =
        when (visibility) {
            ImageVisibility.PUBLIC -> publicBucket
            ImageVisibility.PRIVATE -> privateBucket
        }
}
