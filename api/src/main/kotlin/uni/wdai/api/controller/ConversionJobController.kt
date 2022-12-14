package uni.wdai.api.controller

import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uni.wdai.model.document.ConversionJob
import uni.wdai.model.document.ConversionJob.State.*
import uni.wdai.model.event.ConversionStartEvent
import uni.wdai.repository.ConversionJobRepository
import uni.wdai.service.AwsS3Service
import uni.wdai.service.AwsSqsService
import uni.wdai.util.spring.updateById

@CrossOrigin
@RestController
class ConversionJobController(
    val repository: ConversionJobRepository,
    val s3Service: AwsS3Service,
    val sqsService: AwsSqsService,
) {
    data class CreateJobReq(val commandLine: List<String>)
    data class CreateJobRes(val id: String, val uploadUrl: String)

    @PostMapping("/job")
    suspend fun createJob(@RequestBody req: CreateJobReq): CreateJobRes {
        val job = ConversionJob(commandLine = req.commandLine)
        val uploadUrl = s3Service.generateUploadUrl(job.idString)
        repository.save(job)
        return CreateJobRes(job.idString, uploadUrl)
    }

    @PostMapping("/job/{id}/start")
    suspend fun startJob(@PathVariable id: String) {
        if(!s3Service.isUploaded(id)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File not uploaded")
        val job = repository.updateById(ObjectId(id)) { copy(state = Uploaded) }
        sqsService.send(ConversionStartEvent(id, job.commandLine))
    }

    data class GetJobRes(val state: ConversionJob.State, val downloadUrl: String?)

    @GetMapping("/job/{id}")
    suspend fun getJob(@PathVariable id: String): GetJobRes {
        val job = repository.findById(ObjectId(id))!!
        val downloadUrl = if (job.state == Completed) s3Service.generateDownloadUrl(id) else null
        return GetJobRes(job.state, downloadUrl)
    }
}