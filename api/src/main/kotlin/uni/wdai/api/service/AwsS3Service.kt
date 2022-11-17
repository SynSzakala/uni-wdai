package uni.wdai.api.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import uni.wdai.api.configuration.AwsS3BucketNames
import uni.wdai.api.util.aws.doesObjectExist
import java.time.Duration
import java.util.*

@Service
class AwsS3Service(
    private val client: S3AsyncClient,
    private val presigner: S3Presigner,
    private val bucketNames: AwsS3BucketNames,
    @Value("\${uni.wdai.s3.url.expiration_minutes}") private val urlExpirationMinutes: String,
) {
    private val urlExpiration by lazy { Duration.ofMinutes(urlExpirationMinutes.toLong()) }

    fun generateUploadUrl(id: String) = presigner
        .presignPutObject { presign ->
            presign
                .putObjectRequest { it.inputPath(id) }
                .signatureDuration(urlExpiration)
        }
        .url().toString()

    suspend fun isUploaded(id: String) = client.doesObjectExist { inputPath(id) }

    fun generateDownloadUrl(id: String) = presigner
        .presignGetObject { presign ->
            presign
                .getObjectRequest { it.outputPath(id) }
                .signatureDuration(urlExpiration)
        }
        .url().toString()

    private fun PutObjectRequest.Builder.inputPath(id: String) = bucket(bucketNames.input).key(id)

    private fun HeadObjectRequest.Builder.inputPath(id: String) = bucket(bucketNames.input).key(id)

    private fun GetObjectRequest.Builder.outputPath(id: String) = bucket(bucketNames.output).key(id)
}