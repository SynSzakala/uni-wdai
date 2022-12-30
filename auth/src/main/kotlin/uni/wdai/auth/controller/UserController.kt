package uni.wdai.auth.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import uni.wdai.auth.service.SecretKeyService
import uni.wdai.model.UserData
import uni.wdai.model.document.ConversionJob.State.*
import uni.wdai.model.document.User
import uni.wdai.repository.UserRepository
import uni.wdai.service.AuthTokenService

@CrossOrigin
@RestController
@SecurityRequirement(name = "bearer-key")
class UserController(
    private val repository: UserRepository,
    private val authTokenService: AuthTokenService,
    private val keyService: SecretKeyService,
) {
    data class CreateUserRes(val id: String, val secretKey: String)

    @PostMapping("/user")
    suspend fun createUser(): CreateUserRes {
        val secretKey = keyService.generateKey()
        val user = User(secretKeyHash = keyService.buildHash(secretKey))
        repository.save(user)
        return CreateUserRes(id = user.id.toHexString(), secretKey = secretKey)
    }

    data class AuthUserReq(val secretKey: String)
    data class AuthUserRes(val token: String)

    @PostMapping("/user/{id}/auth")
    suspend fun authUser(@PathVariable id: String, @RequestBody req: AuthUserReq): AuthUserRes {
        val user = findUser(id)
        if(!keyService.isValid(req.secretKey, user.secretKeyHash))
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid secret key")
        return AuthUserRes(token = authTokenService.createToken(user))
    }

    data class UpdateMaxConversionBytesReq(val bytes: Int)

    @PostMapping("/user/{id}/maxConversionBytes")
    @Transactional
    suspend fun updateMaxConversionBytes(
        @PathVariable id: String,
        @RequestBody req: UpdateMaxConversionBytesReq,
        @Parameter(hidden = true) authUser: UserData
    ) {
        if (!authUser.isAdmin) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin authorization required")
        if(req.bytes < 0) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Bytes must be non-negative")
        val user = findUser(id)
        repository.save(user.copy(maxConversionBytes = req.bytes))
    }

    private suspend fun findUser(id: String) =
        repository.findById(ObjectId(id)) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
}