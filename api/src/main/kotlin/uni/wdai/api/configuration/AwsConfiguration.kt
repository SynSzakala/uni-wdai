package uni.wdai.api.configuration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Component
class AwsS3BucketNames(
    @Value("\${uni.wdai.s3.input_bucket}") val input: String,
    @Value("\${uni.wdai.s3.output_bucket}") val output: String,
)

@Component
class AwsSqsQueueUrls(@Value("\${uni.wdai.sqs.start_queue}") val start: String)

@Configuration
class AwsConfiguration {
    private val logger = LoggerFactory.getLogger(AwsConfiguration::class.java)

    @Bean
    fun credentialsProvider(): AwsCredentialsProvider =
        DefaultCredentialsProvider.create().also { logAwsCredentials(it) }

    @Bean
    fun s3Client(credentialsProvider: AwsCredentialsProvider) =
        S3AsyncClient.builder().credentialsProvider(credentialsProvider).build()!!

    @Bean
    fun s3Presigner(credentialsProvider: AwsCredentialsProvider) =
        S3Presigner.builder().credentialsProvider(credentialsProvider).build()!!

    @Bean
    fun sqsClient(credentialsProvider: AwsCredentialsProvider) =
        SqsAsyncClient.builder().credentialsProvider(credentialsProvider).build()!!

    private fun logAwsCredentials(credentialsProvider: AwsCredentialsProvider) {
        val credentials = credentialsProvider.resolveCredentials()
        logger.info("Access key id: ${credentials.accessKeyId()}")
    }
}