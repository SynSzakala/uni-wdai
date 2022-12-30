package uni.wdai.api.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uni.wdai.model.UserData
import uni.wdai.model.document.ConversionJob
import uni.wdai.model.document.ConversionJob.State.*
import uni.wdai.model.event.ConversionStartEvent
import uni.wdai.repository.ConversionJobRepository
import uni.wdai.service.AwsS3Service
import uni.wdai.service.AwsSqsService
import uni.wdai.util.spring.updateById

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearer-key")
class ConversionJobController(
    val repository: ConversionJobRepository,
    val s3Service: AwsS3Service,
    val sqsService: AwsSqsService,
) {
    data class CreateJobReq(
        val sizeBytes: Long,
        val inputFormat: ConversionJob.Format,
        val outputFormat: ConversionJob.Format
    )

    data class CreateJobRes(val id: String, val uploadUrl: String)

    @PostMapping("/job")
    suspend fun createJob(@RequestBody req: CreateJobReq, @Parameter(hidden = true) authUser: UserData): CreateJobRes {
        if (authUser.maxConversionBytes < req.sizeBytes)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversion size too large")
        val job = ConversionJob(inputFormat = req.inputFormat, outputFormat = req.outputFormat, userId = authUser.id)
        val uploadUrl = s3Service.generateUploadUrl(job.idString, req.inputFormat.mimeType, req.sizeBytes)
        repository.save(job)
        return CreateJobRes(job.idString, uploadUrl)
    }

    @PostMapping("/job/{id}/start")
    suspend fun startJob(@PathVariable id: String, @Parameter(hidden = true) authUser: UserData) {
        val job = repository.findById(ObjectId(id))!!
        checkJobUser(job, authUser)
        if (!s3Service.isUploaded(id)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File not uploaded")
        repository.save(job.copy(state = Uploaded))
        sqsService.send(ConversionStartEvent.fromJob(job))
    }

    data class GetJobRes(val state: ConversionJob.State, val downloadUrl: String?)

    @GetMapping("/job/{id}")
    suspend fun getJob(@PathVariable id: String, @Parameter(hidden = true) authUser: UserData): GetJobRes {
        val job = repository.findById(ObjectId(id))!!
        checkJobUser(job, authUser)
        val downloadUrl = if (job.state == Completed) s3Service.generateDownloadUrl(id) else null
        return GetJobRes(job.state, downloadUrl)
    }

    private fun checkJobUser(job: ConversionJob, authUser: UserData) {
        if(job.userId != authUser.id) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized")
    }
}