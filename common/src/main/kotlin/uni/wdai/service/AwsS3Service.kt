package uni.wdai.service

import kotlinx.coroutines.future.await
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import uni.wdai.util.aws.doesObjectExist
import uni.wdai.configuration.AwsS3BucketNames
import java.nio.file.Path
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

    fun generateUploadUrl(id: String, mimeType: String, sizeBytes: Long) = presigner
        .presignPutObject { presign ->
            presign
                .putObjectRequest { it.inputPath(id).contentType(mimeType).contentLength(sizeBytes) }
                .signatureDuration(urlExpiration)
        }
        .url().toString()

    suspend fun isUploaded(id: String) = client.doesObjectExist { inputPath(id) }

    suspend fun downloadToFile(id: String, path: Path) {
        client.getObject({ it.inputPath(id)}, path).await()!!
    }

    suspend fun uploadFromFile(id: String, path: Path, mimeType: String) {
        client.putObject({ it.outputPath(id).contentType(mimeType) }, path).await()!!
    }

    fun generateDownloadUrl(id: String) = presigner
        .presignGetObject { presign ->
            presign
                .getObjectRequest { it.outputPath(id) }
                .signatureDuration(urlExpiration)
        }
        .url().toString()

    private fun GetObjectRequest.Builder.inputPath(id: String) = bucket(bucketNames.input).key(id)

    private fun PutObjectRequest.Builder.inputPath(id: String) = bucket(bucketNames.input).key(id)

    private fun HeadObjectRequest.Builder.inputPath(id: String) = bucket(bucketNames.input).key(id)

    private fun GetObjectRequest.Builder.outputPath(id: String) = bucket(bucketNames.output).key(id)

    private fun PutObjectRequest.Builder.outputPath(id: String) = bucket(bucketNames.output).key(id)
}