package uni.wdai.api.util.aws

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

suspend inline fun S3AsyncClient.doesObjectExist(crossinline block: HeadObjectRequest.Builder.() -> Unit): Boolean {
    return try {
        headObject { block(it) }.await()
        true
    } catch (_: NoSuchKeyException) {
        false
    }
}