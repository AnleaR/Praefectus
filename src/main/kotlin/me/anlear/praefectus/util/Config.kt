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
        get() = props.getProperty("rank_bracket", "DIVINE")
        set(value) {
            props.setProperty("rank_bracket", value)
            save()
        }

    // Window state persistence
    var windowWidth: Int
        get() = props.getProperty("window_width", "1280").toIntOrNull() ?: 1280
        set(value) {
            props.setProperty("window_width", value.toString())
            save()
        }

    var windowHeight: Int
        get() = props.getProperty("window_height", "720").toIntOrNull() ?: 720
        set(value) {
            props.setProperty("window_height", value.toString())
            save()
        }

    var windowX: Int
        get() = props.getProperty("window_x", "-1").toIntOrNull() ?: -1
        set(value) {
            props.setProperty("window_x", value.toString())
            save()
        }

    var windowY: Int
        get() = props.getProperty("window_y", "-1").toIntOrNull() ?: -1
        set(value) {
            props.setProperty("window_y", value.toString())
            save()
        }

    var windowMaximized: Boolean
        get() = props.getProperty("window_maximized", "false").toBoolean()
        set(value) {
            props.setProperty("window_maximized", value.toString())
            save()
        }

    var supportBonus: Boolean
        get() = props.getProperty("support_bonus", "true").toBoolean()
        set(value) {
            props.setProperty("support_bonus", value.toString())
            save()
        }

    var supportBonusValue: Double
        get() = props.getProperty("support_bonus_value", "3.0").toDoubleOrNull() ?: 3.0
        set(value) {
            props.setProperty("support_bonus_value", value.toString())
            save()
        }

    private fun save() {
        configFile.outputStream().use { props.store(it, "Praefectus Config") }
    }
}
