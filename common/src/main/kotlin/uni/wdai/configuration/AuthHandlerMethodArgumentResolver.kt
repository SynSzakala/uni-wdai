package uni.wdai.configuration

import kotlinx.coroutines.reactor.mono
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import uni.wdai.model.UserData
import uni.wdai.service.AuthTokenService
import uni.wdai.util.removePrefixOrNull

@Component
class AuthHandlerMethodArgumentResolver(private val authTokenService: AuthTokenService) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == UserData::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ) = mono<Any?> {
        exchange.request.headers
            .getFirst(HttpHeaders.AUTHORIZATION)
            ?.removePrefixOrNull(bearerPrefix)
            ?.let(::parseToken)
            .also {
                if (it == null && !parameter.isOptional)
                    throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization required")
            }
    }

    private fun parseToken(token: String) = try {
        authTokenService.parseToken(token)
    } catch (e: Throwable) {
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth token", e)
    }

    companion object {
        private const val bearerPrefix = "Bearer "
    }
}