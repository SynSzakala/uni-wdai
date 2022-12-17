package uni.wdai.configuration

import com.mongodb.MongoClientSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.autoconfigure.mongo.MongoPropertiesClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories


@Configuration
@EnableReactiveMongoRepositories
class MongoConfiguration(
    @Value("\${uni.wdai.mongo.ssl.enabled}") val sslEnabled: Boolean
) : AbstractReactiveMongoConfiguration() {
    override fun getDatabaseName() = "default"

    @Bean
    override fun mongoClientSettings() = MongoClientSettings
        .builder()
        .applyToSslSettings { it.enabled(sslEnabled).invalidHostNameAllowed(true) }
        .build()

    @Bean
    fun mongoPropertiesCustomizer(properties: MongoProperties, environment: Environment) =
        MongoPropertiesClientSettingsBuilderCustomizer(properties, environment)
}