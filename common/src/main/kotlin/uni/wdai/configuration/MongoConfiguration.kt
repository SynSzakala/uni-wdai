package uni.wdai.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories
class MongoConfiguration : AbstractReactiveMongoConfiguration() {
    override fun getDatabaseName() = "default"
}