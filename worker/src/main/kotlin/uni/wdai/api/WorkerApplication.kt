package uni.wdai.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import uni.wdai.util.mongo.MongoSslContextHelper

@SpringBootApplication
@EnableReactiveMongoRepositories("uni.wdai")
@ComponentScans(ComponentScan("uni.wdai"))
@PropertySource("classpath:/application.properties")
class WorkerApplication

fun main(args: Array<String>) {
    MongoSslContextHelper.setup()
    runApplication<WorkerApplication>(*args)
}
