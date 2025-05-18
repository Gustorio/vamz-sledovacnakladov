package com.vamz.test

/**
 * Dátová trieda reprezentujúca transakciu v UI vrstve.
 * Používa sa na prenos dát medzi ViewModel a Composable funkciami.
 *
 * @property id Unikátny identifikátor transakcie.
 * @property description Popis transakcie.
 * @property amount Suma transakcie.
 * @property type Typ transakcie ([TransactionType.INCOME] alebo [TransactionType.EXPENSE]).
 * @property timestamp Časová pečiatka transakcie.
 * @property category Kategória transakcie.
 * @property note Voliteľná poznámka.
 */
data class Transaction(
    val id: Int,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val timestamp: Long,
    val category: String,
    val note: String?
)