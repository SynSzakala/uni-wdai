package uni.wdai.repository

import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uni.wdai.model.document.User

@Repository
interface UserRepository : CoroutineCrudRepository<User, ObjectId>