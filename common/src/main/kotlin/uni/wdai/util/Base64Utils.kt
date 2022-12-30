package uni.wdai.util

import java.util.Base64

fun ByteArray.encodeBase64() = Base64.getEncoder().encodeToString(this)!!

fun String.decodeBase64() = Base64.getDecoder().decode(this)!!