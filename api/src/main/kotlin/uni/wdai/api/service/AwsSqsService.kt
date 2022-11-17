package uni.wdai.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uni.wdai.api.configuration.AwsSqsQueueUrls
import uni.wdai.api.model.event.ConversionStartEvent

@Service
class AwsSqsService(
    private val client: SqsAsyncClient,
    private val queueUrls: AwsSqsQueueUrls,
    private val objectMapper: ObjectMapper
) {
    suspend fun send(event: ConversionStartEvent) {
        client.sendMessage { it.queueUrl(queueUrls.start).messageBody(objectMapper.writeValueAsString(event)) }.await()
    }
}