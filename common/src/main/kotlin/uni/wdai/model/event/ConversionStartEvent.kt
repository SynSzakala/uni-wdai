package uni.wdai.model.event

import uni.wdai.model.document.ConversionJob

data class ConversionStartEvent(
    val id: String,
    val inputFormat: ConversionJob.Format,
    val outputFormat: ConversionJob.Format
) {
    companion object {
        fun fromJob(job: ConversionJob) =
            ConversionStartEvent(id = job.idString, inputFormat = job.inputFormat, outputFormat = job.outputFormat)
    }
}