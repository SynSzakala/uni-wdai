package uni.wdai.auth.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uni.wdai.model.document.User
import uni.wdai.repository.UserRepository
import uni.wdai.util.spring.logger
import javax.annotation.PostConstruct

@OptIn(DelicateCoroutinesApi::class)
@Service
class AdminUserCreationService(
    private val repository: UserRepository,
    private val keyService: SecretKeyService,
    @Value("\${uni.wdai.auth.admin.id}") private val adminUserId: String,
    @Value("\${uni.wdai.auth.admin.secret_key}") private val adminSecretKey: String,
) {
    @PostConstruct
    fun initialize() = GlobalScope.launch { createAdminUser() }

    private suspend fun createAdminUser() {
        if(!repository.existsById(ObjectId(adminUserId))) {
            val user = User(
                id = ObjectId(adminUserId),
                secretKeyHash = keyService.buildHash(adminSecretKey),
                isAdmin = true
            )
            repository.save(user)
            logger.info("Created admin user $adminUserId")
        } else {
            logger.info("Admin user $adminUserId already exists")
        }
    }
}