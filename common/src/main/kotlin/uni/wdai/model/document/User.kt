package uni.wdai.model.document

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import uni.wdai.model.UserData

@Document
data class User(
    @Id override val id: ObjectId = ObjectId(),
    override val maxConversionBytes: Long = 0,
    override val isAdmin: Boolean = false,
    val secretKeyHash: String,
) : UserData