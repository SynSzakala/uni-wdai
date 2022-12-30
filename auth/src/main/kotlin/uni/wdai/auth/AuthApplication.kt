package uni.wdai.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@SpringBootApplication
@EnableReactiveMongoRepositories("uni.wdai")
@ComponentScans(ComponentScan("uni.wdai"))
@PropertySource("classpath:/application.properties")
class AuthApplication

fun main(args: Array<String>) {
    runApplication<AuthApplication>(*args)
}
