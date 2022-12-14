package uni.wdai.repository

import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import uni.wdai.model.document.ConversionJob

@Repository
interface ConversionJobRepository : CoroutineCrudRepository<ConversionJob, ObjectId>