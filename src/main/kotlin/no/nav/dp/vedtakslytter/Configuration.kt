package no.nav.dp.vedtakslytter

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private val localProperties = ConfigurationMap(
        mapOf(
                "application.profile" to "LOCAL",
                "application.httpPort" to "8099"
        )
)

private val devProperties = ConfigurationMap(
        mapOf(
                "application.profile" to "DEV",
                "application.httpPort" to "8099"
        )
)

private val prodProperties = ConfigurationMap(
        mapOf(
                "application.profile" to "PROD",
                "application.httpPort" to "8099"
        )
)

data class Application(
    val httpPort: Int = config()[Key("application.httpPort", intType)],
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) }
)

enum class Profile {
    LOCAL, DEV, PROD
}

data class Configuration(
    val application: Application = Application()
)

fun getEnvOrProp(propName: String): String? {
    return System.getenv(propName) ?: System.getProperty(propName)
}

private fun config() = when (getEnvOrProp("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
}