package uni.wdai.api.util.spring

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

suspend inline fun <T, ID> CoroutineCrudRepository<T, ID>.updateById(id: ID, block: T.() -> T) =
    save(block(findById(id)!!))