package uni.wdai.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uni.wdai.model.UserData
import uni.wdai.model.fromStringMap
import uni.wdai.model.toStringMap
import uni.wdai.util.decodeBase64

@Service
class AuthTokenService(@Value("\${uni.wdai.auth.secret_key}") private val secretKeyString: String) {
    private val secretKey by lazy { Keys.hmacShaKeyFor(secretKeyString.decodeBase64()) }
    private val parser by lazy { createJwtParser() }

    fun createToken(user: UserData) = createJwtBuilder()
        .setClaims(user.toStringMap())
        .compact()!!

    @Suppress("UNCHECKED_CAST")
    fun parseToken(token: String) =
        parser.parseClaimsJws(token).run { UserData.fromStringMap(body as Map<String, String>) }

    private fun createJwtBuilder() = Jwts.builder().signWith(secretKey).serializeToJsonWith(JacksonSerializer())

    private fun createJwtParser() = Jwts.parserBuilder().setSigningKey(secretKey).build()
}