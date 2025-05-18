package com.vamz.test.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


/**
 * Data Access Object (DAO) pre operácie s tabuľkou 'transactions'.
 * Poskytuje metódy pre vkladanie, aktualizáciu, mazanie a získavanie transakcií.
 */
@Dao
interface TransactionDao {

    /**
     * Vloží novú transakciu do databázy. Ak transakcia s rovnakým primárnym kľúčom
     * už existuje, bude nahradená.
     *
     * @param transaction Entita transakcie, ktorá sa má vložiť.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    /**
     * Aktualizuje existujúcu transakciu v databáze.
     *
     * @param transaction Entita transakcie, ktorá sa má aktualizovať.
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * Zmaže transakciu z databázy.
     *
     * @param transaction Entita transakcie, ktorá sa má zmazať.
     */
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    /**
     * Získa všetky transakcie z databázy, zoradené podľa časovej pečiatky od najnovšej po najstaršiu.
     * Vráti Kotlin Flow, ktorý emituje zoznam transakcií pri každej zmene v tabuľke.
     *
     * @return Flow zoznamu všetkých [TransactionEntity].
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Získa transakciu podľa jej unikátneho ID.
     *
     * @param id ID transakcie, ktorú chceme získať.
     * @return Entita transakcie [TransactionEntity] alebo null, ak sa nenašla.
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    /**
     * Získa transakcie, ktorých časová pečiatka spadá do zadaného časového rozsahu.
     * Transakcie sú zoradené podľa časovej pečiatky od najnovšej po najstaršiu.
     * Vráti Kotlin Flow, ktorý emituje zoznam transakcií pri každej zmene v tabuľke
     * v rámci daného rozsahu.
     *
     * @param startTime Začiatok časového rozsahu (v milisekundách od epochy).
     * @param endTime Koniec časového rozsahu (v milisekundách od epochy).
     * @return Flow zoznamu [TransactionEntity] v rámci zadaného obdobia.
     */
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetweenTimestamps(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    /**
     * Zmaže všetky transakcie z tabuľky 'transactions'.
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}