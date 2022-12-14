package uni.wdai.util.spring

import org.slf4j.LoggerFactory

inline val <reified T> T.logger get() = LoggerFactory.getLogger(T::class.java)!!