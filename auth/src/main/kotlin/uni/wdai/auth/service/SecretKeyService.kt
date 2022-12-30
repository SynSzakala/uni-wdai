package uni.wdai.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import uni.wdai.auth.util.crypto.SecureRandom
import uni.wdai.util.encodeBase64

@Service
class SecretKeyService {
    private val bcrypt by lazy { BCrypt.withDefaults() }
    private val verifier by lazy { BCrypt.verifyer() }

    fun generateKey() = SecureRandom.nextBytes(secretKeyLength).encodeBase64()

    fun buildHash(key: String) = bcrypt.hashToString(cost, key.toCharArray())!!

    fun isValid(key: String, hash: String) = verifier.verify(key.toCharArray(), hash).verified

    companion object {
        private const val cost = 12
        private const val secretKeyLength = 32
    }
}