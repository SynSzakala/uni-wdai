package uni.wdai.auth.util.crypto

import java.security.SecureRandom
import kotlin.random.asKotlinRandom

val SecureRandom = SecureRandom().asKotlinRandom()
