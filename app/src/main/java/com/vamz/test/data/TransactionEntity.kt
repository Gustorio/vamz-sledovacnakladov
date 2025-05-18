package com.vamz.test.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vamz.test.TransactionType

/**
 * Entita databázy reprezentujúca jednu finančnú transakciu.
 * Táto trieda mapuje na tabuľku 'transactions' v Room databáze.
 *
 * @property id Unikátny identifikátor transakcie. Je to primárny kľúč a automaticky sa generuje.
 * @property description Stručný popis transakcie (napr. "Nákup potravín", "Výplata").
 * @property amount Suma transakcie. Pre výdavky by mala byť záporná, pre príjmy kladná.
 * @property type Typ transakcie ([TransactionType.INCOME] alebo [TransactionType.EXPENSE]).
 * @property timestamp Časová pečiatka transakcie v milisekundách od epochy. Používa sa na uloženie dátumu a času. Predvolená hodnota je aktuálny čas.
 * @property category Kategória transakcie (napr. "Potraviny", "Mzda", "Bývanie").
 * @property note Voliteľná dodatočná poznámka k transakcii. Môže byť null.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val note: String?
)