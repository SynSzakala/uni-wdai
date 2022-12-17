package uni.wdai.util.mongo

import org.slf4j.LoggerFactory
import org.springframework.util.ObjectUtils
import java.io.File
import java.io.FileOutputStream
import java.lang.invoke.MethodHandles
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object MongoSslContextHelper {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private const val DEFAULT_SSL_CERTIFICATE = "/rds-combined-ca-bundle.pem"
    private const val SSL_CERTIFICATE = "sslCertificate"
    private const val KEY_STORE_TYPE = "JKS"
    private const val KEY_STORE_PROVIDER = "SUN"
    private const val KEY_STORE_FILE_PREFIX = "sys-connect-via-ssl-test-cacerts"
    private const val KEY_STORE_FILE_SUFFIX = ".jks"
    private const val DEFAULT_KEY_STORE_PASSWORD = "changeit"
    private const val SSL_TRUST_STORE = "javax.net.ssl.trustStore"
    private const val SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword"
    private const val SSL_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType"

    fun setup() {
        try {
            var sslCertificate = System.getProperty(SSL_CERTIFICATE)
            if (ObjectUtils.isEmpty(sslCertificate)) {
                sslCertificate = DEFAULT_SSL_CERTIFICATE
            }
            logger.info(" ssl certificate path {}", sslCertificate)
            System.setProperty(SSL_TRUST_STORE, createKeyStoreFile(sslCertificate))
            System.setProperty(SSL_TRUST_STORE_TYPE, KEY_STORE_TYPE)
            System.setProperty(SSL_TRUST_STORE_PASSWORD, DEFAULT_KEY_STORE_PASSWORD)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createKeyStoreFile(sslCertificate: String): String =
        createKeyStoreFile(createCertificate(sslCertificate)).path

    private fun createCertificate(sslCertificate: String): X509Certificate {
        val certFactory = CertificateFactory.getInstance("X.509")
        return javaClass.getResourceAsStream(sslCertificate).use { certInputStream ->
            certFactory.generateCertificate(certInputStream) as X509Certificate
        }
    }

    @Throws(Exception::class)
    private fun createKeyStoreFile(rootX509Certificate: X509Certificate): File {
        val keyStoreFile = File.createTempFile(KEY_STORE_FILE_PREFIX, KEY_STORE_FILE_SUFFIX)
        FileOutputStream(keyStoreFile.path).use { fos ->
            val ks = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER)
            ks.load(null)
            ks.setCertificateEntry("rootCaCertificate", rootX509Certificate)
            ks.store(fos, DEFAULT_KEY_STORE_PASSWORD.toCharArray())
        }
        return keyStoreFile
    }
}