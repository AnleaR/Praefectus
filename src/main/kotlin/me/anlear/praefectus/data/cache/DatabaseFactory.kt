package me.anlear.praefectus.data.cache

import me.anlear.praefectus.data.cache.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    private lateinit var db: Database

    fun init() {
        val dbDir = File(System.getProperty("user.home"), ".praefectus")
        dbDir.mkdirs()
        val dbPath = File(dbDir, "cache.db").absolutePath

        db = Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(HeroesTable, HeroStatsTable, HeroMatchupsTable, CacheMetadataTable)
        }
    }

    fun getDatabase(): Database = db
}
