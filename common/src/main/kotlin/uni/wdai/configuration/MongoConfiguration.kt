package uni.wdai.configuration

import com.mongodb.ReadPreference
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MongoConfiguration {
    @Bean
    fun clientSettingsCustomizer() = MongoClientSettingsBuilderCustomizer {
        it.readPreference(ReadPreference.secondaryPreferred()).retryWrites(false)
    }
}