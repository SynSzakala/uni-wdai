package uni.wdai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uni.wdai.configuration.AwsSqsQueueUrls
import uni.wdai.model.event.ConversionStartEvent

@Service
class AwsSqsService(
    private val client: SqsAsyncClient,
    private val queueUrls: AwsSqsQueueUrls,
    private val objectMapper: ObjectMapper
) {
    suspend fun send(event: ConversionStartEvent) {
        client.sendMessage { it.queueUrl(queueUrls.start).messageBody(objectMapper.writeValueAsString(event)) }.await()
    }

    fun receiveSerial() = flow<ConversionStartEvent> {
        while (true) {
            val message =
                client.receiveMessage { it.queueUrl(queueUrls.start).maxNumberOfMessages(1).waitTimeSeconds(20) }
                    .await().messages().firstOrNull()
            if (message != null) {
                coroutineScope {
                    val job = launch {
                        while (true) {
                            delay(1000)
                            client.changeMessageVisibility {
                                it.queueUrl(queueUrls.start).receiptHandle(message.receiptHandle())
                                    .visibilityTimeout(1000)
                            }
                        }
                    }
                    emit(objectMapper.readValue(message.body()))
                    client.deleteMessage { it.queueUrl(queueUrls.start).receiptHandle(message.receiptHandle()) }
                    job.cancel()
                }
            }
        }
    }
}