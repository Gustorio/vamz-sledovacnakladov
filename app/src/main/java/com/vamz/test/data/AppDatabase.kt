package com.vamz.test.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room databáza pre správu finančných transakcií.
 * Definuje entitu [TransactionEntity] a poskytuje prístup k [TransactionDao].
 *
 * @property entities Zoznam entít zahrnutých v databáze.
 * @property version Verzia schémy databázy. Pri zmene schémy je potrebné zvýšiť verziu.
 * @property exportSchema Určuje, či sa má exportovať schéma databázy.
 */
@Database(entities = [TransactionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Abstraktná metóda, ktorá vracia Data Access Object (DAO) pre transakcie.
     * Room automaticky implementuje túto metódu.
     *
     * @return Inštancia [TransactionDao].
     */
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Vráti jedinú inštanciu Room databázy pre daný kontext aplikácie.
         * Ak inštancia ešte neexistuje, vytvorí novú v synchronizovanom bloku
         * pre bezpečný prístup z viacerých vlákien.
         *
         * @param context Kontext aplikácie použitý na získanie databázy.
         * @return Jediná inštancia [AppDatabase].
         */
        fun getDatabase(context: Context): AppDatabase {
            // Ak INSTANCE nie je null, vráťte ju, inak vytvorte novú inštanciu
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}