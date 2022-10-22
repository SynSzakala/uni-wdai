package uni.wdai.api.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration

@Configuration
class MongoConfiguration : AbstractReactiveMongoConfiguration() {
    override fun getDatabaseName() = "default"
}