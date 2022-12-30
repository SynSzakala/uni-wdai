package uni.wdai.util

fun String.removePrefixOrNull(prefix: String) = takeIf { it.startsWith(prefix) }?.removePrefix(prefix)