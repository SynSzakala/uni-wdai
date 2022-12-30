package uni.wdai.model

import org.bson.types.ObjectId

interface UserData {
    val id: ObjectId
    val isAdmin: Boolean
    val maxConversionBytes: Long

    data class Basic(
        override val id: ObjectId,
        override val isAdmin: Boolean,
        override val maxConversionBytes: Long,
    ) : UserData

    companion object
}

fun UserData.Companion.fromStringMap(map: Map<String, String>) = UserData.Basic(
    id = ObjectId(map["id"]),
    isAdmin = map["isAdmin"]!!.toBoolean(),
    maxConversionBytes = map["maxConversionBytes"]!!.toLong()
)

fun UserData.toStringMap() = mapOf(
    "id" to id.toHexString(),
    "isAdmin" to isAdmin.toString(),
    "maxConversionBytes" to maxConversionBytes.toString()
)