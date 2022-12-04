package uni.wdai.model.document

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class ConversionJob(
    @Id val id: ObjectId = ObjectId.get(),
    val commandLine: List<String>,
    val state: State = State.Created,
) {
    enum class State {
        Created,
        Uploaded,
        Completed,
        Failed
    }

    val idString get() = id.toHexString()!!
}