package me.anlear.praefectus.util

import java.io.File
import java.util.Properties

object Config {
    private val configDir = File(System.getProperty("user.home"), ".praefectus")
    private val configFile = File(configDir, "config.properties")
    private val props = Properties()

    init {
        configDir.mkdirs()
        if (configFile.exists()) {
            configFile.inputStream().use { props.load(it) }
        }
    }

    var apiToken: String
        get() = props.getProperty("api_token", "")
        set(value) {
            props.setProperty("api_token", value)
            save()
        }

    var language: String
        get() = props.getProperty("language", "EN")
        set(value) {
            props.setProperty("language", value)
            save()
        }

    var rankBracket: String
        get() = props.getProperty("rank_bracket", "DIVINE_IMMORTAL")
        set(value) {
            props.setProperty("rank_bracket", value)
            save()
        }

    private fun save() {
        configFile.outputStream().use { props.store(it, "Praefectus Config") }
    }
}
