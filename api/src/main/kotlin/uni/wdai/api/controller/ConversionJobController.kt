package uni.wdai.api.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uni.wdai.api.model.document.ConversionJob
import uni.wdai.api.repository.ConversionJobRepository

@RestController
class ConversionJobController(val repository: ConversionJobRepository) {
    data class CreateJobReq(val commandLine: String)
    data class CreateJobRes(val id: String, val uploadUrl: String)

    @PostMapping("/job")
    suspend fun createJob(@RequestBody req: CreateJobReq): CreateJobRes {
        val job = ConversionJob(commandLine = req.commandLine)
        repository.save(job)
        return CreateJobRes(id = job.id.toHexString(), uploadUrl = "TODO")
    }
}