package uni.wdai.model.document

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.util.MimeType

@Document
data class ConversionJob(
    @Id val id: ObjectId = ObjectId.get(),
    val inputFormat: Format,
    val outputFormat: Format,
    val state: State = State.Created,
    val userId: ObjectId,
) {
    enum class State {
        Created,
        Uploaded,
        Completed,
        Failed
    }

    enum class Format(val mimeType: String) {
        Mp4("video/mp4"),
        Mp3("audio/mpeg"),
        Aac("audio/aac"),
    }

    val idString get() = id.toHexString()!!
}