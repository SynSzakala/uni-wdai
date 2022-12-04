package uni.wdai.api.service

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uni.wdai.model.document.ConversionJob
import uni.wdai.model.event.ConversionStartEvent
import uni.wdai.repository.ConversionJobRepository
import uni.wdai.service.AwsS3Service
import uni.wdai.service.AwsSqsService
import uni.wdai.util.spring.updateById
import javax.annotation.PostConstruct
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

@OptIn(DelicateCoroutinesApi::class)
@Service
class ConversionService(
    val sqsService: AwsSqsService,
    val s3Service: AwsS3Service,
    val repository: ConversionJobRepository,
) : CoroutineScope by GlobalScope {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun start() = sqsService.receiveSerial().onEach { handle(it) }.launchIn(this)

    private suspend fun handle(event: ConversionStartEvent) {
        logger.info("Starting conversion ${event.id} (commandLine=${event.commandLine})")
        try {
            s3Service.downloadToFile(event.id, path = Path("input"))
            val result = process("ffmpeg", *event.commandLine.toTypedArray(), stdout = Redirect.CAPTURE, stderr = Redirect.CAPTURE)
            when (result.resultCode) {
                0 -> {
                    s3Service.uploadFromFile(event.id, path = Path("output"))
                    repository.updateById(ObjectId(event.id)) { copy(state = ConversionJob.State.Completed) }
                    logger.info("Conversion success for ${event.id}")
                }

                else -> {
                    repository.updateById(ObjectId(event.id)) { copy(state = ConversionJob.State.Failed) }
                    logger.info("Conversion failure for ${event.id} with\n${result.output.joinToString("\n")}")
                }
            }
        } finally {
            Path("input").deleteIfExists()
            Path("output").deleteIfExists()
        }
    }
}